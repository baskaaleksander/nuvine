package com.baskaaleksander.nuvine.domain.service.chunker;

import com.baskaaleksander.nuvine.domain.model.Chunk;
import com.baskaaleksander.nuvine.domain.model.ExtractedDocument;
import com.baskaaleksander.nuvine.domain.model.SentenceSpan;
import com.baskaaleksander.nuvine.domain.model.WordSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChunkerService {

    private final Tokenizer tokenizer;

    public List<Chunk> chunkDocument(ExtractedDocument extractedDocument, UUID documentId) {
        String rawText = extractedDocument.text();
        if (rawText == null || rawText.isBlank()) {
            return List.of();
        }

        return chunkPage(rawText, documentId, 0);
    }

    private List<Chunk> chunkPage(String rawText, UUID documentId, int page) {

        int maxTokens = 700;
        int overlapTokens = 120;

        List<SentenceSpan> sentences = splitIntoSentenceSpans(rawText);

        List<Chunk> out = new ArrayList<>();

        if (sentences.isEmpty()) {
            return out;
        }

        int chunkStartChar = sentences.get(0).start();

        List<SentenceSpan> current = new ArrayList<>();
        int currentTokens = 0;

        for (SentenceSpan s : sentences) {
            int sTokens = tokenizer.count(s.text());

            if (sTokens > maxTokens) {
                if (!current.isEmpty()) {
                    Chunk ch = buildChunkFromSpans(rawText, documentId, page, current, chunkStartChar);
                    out.add(ch);
                }

                out.addAll(splitLongSentenceToChunks(rawText, documentId, page, s, maxTokens, overlapTokens));

                chunkStartChar = s.end();
                continue;
            }

            if (currentTokens + sTokens <= maxTokens) {
                if (current.isEmpty()) chunkStartChar = s.start();
                current.add(s);
                currentTokens += sTokens;
            } else {
                Chunk ch = buildChunkFromSpans(rawText, documentId, page, current, chunkStartChar);
                out.add(ch);

                int overlapStart = computeOverlapStartChar(rawText, current, overlapTokens);
                overlapStart = alignToWordBoundaryLeft(rawText, overlapStart);

                List<SentenceSpan> overlapped = sliceSpansForOverlap(rawText, current, overlapStart);
                current = new ArrayList<>(overlapped);
                currentTokens = tokenizer.count(concatText(rawText, overlapped, overlapStart, overlapped.get(overlapped.size() - 1).end()));

                if (currentTokens + sTokens > maxTokens) {
                    Chunk ch2 = buildChunkFromSpans(rawText, documentId, page, current, overlapStart);
                    out.add(ch2);
                    chunkStartChar = s.start();
                    current = new ArrayList<>(List.of(s));
                    currentTokens = sTokens;
                } else {
                    if (current.isEmpty()) chunkStartChar = s.start();
                    else chunkStartChar = overlapStart;
                    current.add(s);
                    currentTokens += sTokens;
                }
            }
        }

        if (!current.isEmpty()) {
            Chunk ch = buildChunkFromSpans(rawText, documentId, page, current, chunkStartChar);
            out.add(ch);
        }

        return out;
    }

    private int alignToWordBoundaryLeft(String t, int pos) {
        if (pos <= 0) return 0;
        int i = pos;
        while (i > 0 && !Character.isWhitespace(t.charAt(i - 1))) i--;
        return i;
    }

    private List<SentenceSpan> splitIntoSentenceSpans(String rawText) {
        Pattern p = Pattern.compile(".+?(?<=[.!?])(?=\\s+|$)", Pattern.DOTALL | Pattern.UNICODE_CASE);
        Matcher m = p.matcher(rawText);

        List<SentenceSpan> spans = new ArrayList<>();

        while (m.find()) {
            int s = m.start();
            int e = m.end();
            String piece = rawText.substring(s, e).trim();

            if (!piece.isEmpty()) {
                int leading = leadingWs(rawText, s, e);
                int trailing = trailingWs(rawText, s, e);
                int startAdj = s + leading;
                int endAdj = e - trailing;

                spans.add(new SentenceSpan(startAdj, endAdj, rawText.substring(startAdj, endAdj)));
            }
        }

        if (spans.isEmpty() && !rawText.isBlank()) {
            spans.add(new SentenceSpan(0, rawText.length(), rawText));
        }

        return spans;
    }

    private int leadingWs(String t, int s, int e) {
        int i = s;
        while (i < e && Character.isWhitespace(t.charAt(i))) i++;
        return i - s;
    }

    private int trailingWs(String t, int s, int e) {
        int i = e - 1;
        while (i >= s && Character.isWhitespace(t.charAt(i))) i--;
        return (e - 1) - i;
    }

    private Chunk buildChunkFromSpans(String text, UUID docId, int page, List<SentenceSpan> spans, int chunkStartChar) {
        int end = spans.get(spans.size() - 1).end();
        String content = text.substring(chunkStartChar, end);

        return new Chunk(docId, page, chunkStartChar, end, content);
    }

    private int computeOverlapStartChar(String text, List<SentenceSpan> spans, int overlapTokens) {
        int chunkStart = spans.get(0).start();
        int chunkEnd = spans.get(spans.size() - 1).end();

        int total = tokenizer.count(text.substring(chunkStart, chunkEnd));
        if (total <= overlapTokens) return chunkStart;

        int lo = chunkStart, hi = chunkEnd, ans = chunkStart;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int suffixTokens = tokenizer.count(text.substring(mid, chunkEnd));
            if (suffixTokens >= overlapTokens) {
                ans = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        return ans;
    }

    private List<SentenceSpan> sliceSpansForOverlap(String text, List<SentenceSpan> spans, int overlapStart) {
        int chunkEnd = spans.get(spans.size() - 1).end();
        List<SentenceSpan> out = new ArrayList<>();
        for (SentenceSpan ss : spans) {
            if (ss.end() <= overlapStart) continue;
            int s = Math.max(ss.start(), overlapStart);
            int e = ss.end();
            if (s < e) {
                out.add(new SentenceSpan(s, e, text.substring(s, e)));
            }
        }
        if (out.isEmpty()) {
            out.add(new SentenceSpan(overlapStart, chunkEnd, text.substring(overlapStart, chunkEnd)));
        }
        return out;
    }

    private List<Chunk> splitLongSentenceToChunks(String text, UUID docId, int page, SentenceSpan longSentence, int maxTokens, int overlapTokens) {

        List<Chunk> out = new ArrayList<>();
        List<WordSpan> words = wordSpans(longSentence.text(), longSentence.start());

        int n = words.size();
        if (n == 0) return out;

        int startIdx = 0;
        while (startIdx < n) {
            int endIdx = startIdx;
            while (endIdx < n) {
                String segment = sliceWords(text, words, startIdx, endIdx);
                int tokens = tokenizer.count(segment);
                if (tokens > maxTokens) break;
                endIdx++;
            }
            if (endIdx == startIdx) {
                int hardStart = words.get(startIdx).start();
                int hardEnd = Math.min(longSentence.end(), hardStart + Math.min(2000, maxTokens * 4));
                String content = text.substring(hardStart, hardEnd);
                out.add(new Chunk(docId, page, hardStart, hardEnd, content));
                startIdx++;
                continue;
            }
            int segStart = words.get(startIdx).start();
            int segEnd = words.get(endIdx - 1).end();

            String content = text.substring(segStart, segEnd);
            out.add(new Chunk(docId, page, segStart, segEnd, content));

            if (endIdx >= n) break;

            int lo = startIdx, hi = endIdx, ans = startIdx;

            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                String suffix = sliceWords(text, words, mid, endIdx - 1);
                int tokens = tokenizer.count(suffix);

                if (tokens >= overlapTokens) {
                    ans = mid;
                    lo = mid + 1;

                } else {
                    hi = mid - 1;
                }
            }
            startIdx = Math.max(ans, endIdx - 1);
        }
        return out;
    }

    private List<WordSpan> wordSpans(String text, int baseOffset) {
        List<WordSpan> out = new ArrayList<>();
        Matcher m = Pattern.compile("\\S+").matcher(text);
        while (m.find()) {
            out.add(new WordSpan(baseOffset + m.start(), baseOffset + m.end()));
        }

        return out;
    }

    private String sliceWords(String pageText, List<WordSpan> wordSpans, int i, int jInclusive) {
        int start = wordSpans.get(i).start();
        int end = wordSpans.get(jInclusive).end();

        return pageText.substring(start, end);
    }

    private String concatText(String pageText, List<SentenceSpan> spans, int start, int end) {
        if (spans.isEmpty()) return "";
        return pageText.substring(start, end);
    }
}