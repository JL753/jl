package com.iflytek.smartprep.rag.util;

import com.iflytek.smartprep.rag.model.DocumentChunk;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档智能分块工具
 * 实现带重叠窗口的滑动切分策略，确保知识上下文不丢失
 */
@Component
public class DocumentChunker {

    // 默认配置
    private static final int DEFAULT_CHUNK_SIZE = 500;        // 每块字符数
    private static final int DEFAULT_OVERLAP_SIZE = 100;      // 重叠字符数
    private static final int MIN_CHUNK_SIZE = 100;            // 最小块大小

    // 章节标题正则（匹配"第X章"、"Chapter X"、"一、"等）
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
        "^(第[一二三四五六七八九十百\\d]+章|Chapter\\s+\\d+|[一二三四五六七八九十]+[、.]|\\d+[、.]|#+\\s+)(.+)$",
        Pattern.MULTILINE
    );

    /**
     * 使用默认配置切分文档
     */
    public List<DocumentChunk> chunkDocument(Long documentId, String documentTitle, String course, String content, String tag) {
        return chunkDocument(documentId, documentTitle, course, content, tag, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE);
    }

    /**
     * 智能切分文档
     *
     * @param documentId 文档ID
     * @param documentTitle 文档标题
     * @param course 课程名称
     * @param content 文档内容
     * @param tag 标签
     * @param chunkSize 每块大小
     * @param overlapSize 重叠大小
     * @return 文档块列表
     */
    public List<DocumentChunk> chunkDocument(Long documentId, String documentTitle, String course,
                                            String content, String tag, int chunkSize, int overlapSize) {
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 预处理：统一换行符，去除多余空白
        content = normalizeContent(content);

        // 检测章节结构
        List<ChapterSection> chapters = detectChapters(content);

        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        if (chapters.isEmpty()) {
            // 无章节结构，直接按滑动窗口切分
            chunks.addAll(chunkByOverlappingWindow(documentId, documentTitle, course, content,
                                                   tag, chunkSize, overlapSize, chunkIndex, null));
        } else {
            // 按章节切分，每个章节内部再使用滑动窗口
            for (ChapterSection chapter : chapters) {
                String chapterContent = content.substring(chapter.startPos, chapter.endPos);
                List<DocumentChunk> chapterChunks = chunkByOverlappingWindow(
                    documentId, documentTitle, course, chapterContent,
                    tag, chunkSize, overlapSize, chunkIndex, chapter.title
                );
                chunks.addAll(chapterChunks);
                chunkIndex += chapterChunks.size();
            }
        }

        return chunks;
    }

    /**
     * 使用重叠滑动窗口切分文本
     */
    private List<DocumentChunk> chunkByOverlappingWindow(Long documentId, String documentTitle, String course,
                                                         String content, String tag, int chunkSize, int overlapSize,
                                                         int startIndex, String chapter) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int contentLength = content.length();
        int currentPos = 0;
        int chunkIndex = startIndex;

        while (currentPos < contentLength) {
            // 计算当前块的结束位置
            int endPos = Math.min(currentPos + chunkSize, contentLength);

            // 如果不是最后一块，尝试在句子边界处切分
            if (endPos < contentLength) {
                endPos = findSentenceBoundary(content, endPos, currentPos + chunkSize / 2);
            }

            // 提取文本块
            String chunkContent = content.substring(currentPos, endPos).trim();

            // 只保留有效内容的块
            if (chunkContent.length() >= MIN_CHUNK_SIZE) {
                DocumentChunk chunk = DocumentChunk.builder()
                    .documentId(documentId)
                    .documentTitle(documentTitle)
                    .course(course)
                    .chunkIndex(chunkIndex++)
                    .content(chunkContent)
                    .chapter(chapter)
                    .tag(tag)
                    .startPosition(currentPos)
                    .endPosition(endPos)
                    .createdAt(System.currentTimeMillis())
                    .build();
                chunks.add(chunk);
            }

            // 移动到下一个位置（考虑重叠）
            currentPos = endPos - overlapSize;

            // 防止无限循环
            if (currentPos <= endPos - chunkSize + overlapSize) {
                currentPos = endPos;
            }
        }

        return chunks;
    }

    /**
     * 检测文档中的章节结构
     */
    private List<ChapterSection> detectChapters(String content) {
        List<ChapterSection> chapters = new ArrayList<>();
        Matcher matcher = CHAPTER_PATTERN.matcher(content);

        int lastEnd = 0;
        String lastTitle = null;

        while (matcher.find()) {
            int currentStart = matcher.start();
            String currentTitle = matcher.group(2).trim();

            // 保存上一个章节
            if (lastTitle != null) {
                chapters.add(new ChapterSection(lastTitle, lastEnd, currentStart));
            }

            lastTitle = currentTitle;
            lastEnd = currentStart;
        }

        // 保存最后一个章节
        if (lastTitle != null) {
            chapters.add(new ChapterSection(lastTitle, lastEnd, content.length()));
        }

        return chapters;
    }

    /**
     * 在句子边界处切分（优先在句号、问号、感叹号处切分）
     */
    private int findSentenceBoundary(String content, int idealPos, int minPos) {
        // 句子结束标记
        String[] sentenceEnds = {"。", "！", "？", ".", "!", "?", "\n\n"};

        // 在理想位置前后搜索句子边界
        int searchStart = Math.max(minPos, idealPos - 50);
        int searchEnd = Math.min(content.length(), idealPos + 50);

        int bestPos = idealPos;
        int bestDistance = Integer.MAX_VALUE;

        for (int i = searchStart; i < searchEnd; i++) {
            for (String end : sentenceEnds) {
                if (content.startsWith(end, i)) {
                    int distance = Math.abs(i - idealPos);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestPos = i + end.length();
                    }
                }
            }
        }

        return bestPos;
    }

    /**
     * 标准化文本内容
     */
    private String normalizeContent(String content) {
        // 统一换行符
        content = content.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");

        // 去除多余空白（保留段落结构）
        content = content.replaceAll("[ \\t]+", " ");
        content = content.replaceAll("\\n{3,}", "\n\n");

        return content.trim();
    }

    /**
     * 章节信息内部类
     */
    private static class ChapterSection {
        String title;
        int startPos;
        int endPos;

        ChapterSection(String title, int startPos, int endPos) {
            this.title = title;
            this.startPos = startPos;
            this.endPos = endPos;
        }
    }

    /**
     * 按页码切分（用于PDF文档）
     */
    public List<DocumentChunk> chunkByPages(Long documentId, String documentTitle, String course,
                                           List<PageContent> pages, String tag) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (PageContent page : pages) {
            // 如果页面内容过长，进一步切分
            if (page.content.length() > DEFAULT_CHUNK_SIZE) {
                List<DocumentChunk> pageChunks = chunkByOverlappingWindow(
                    documentId, documentTitle, course, page.content,
                    tag, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP_SIZE, chunkIndex, null
                );

                // 设置页码信息
                for (DocumentChunk chunk : pageChunks) {
                    chunk.setPageNumber(page.pageNumber);
                    chunks.add(chunk);
                }
                chunkIndex += pageChunks.size();
            } else {
                // 页面内容较短，直接作为一个块
                DocumentChunk chunk = DocumentChunk.builder()
                    .documentId(documentId)
                    .documentTitle(documentTitle)
                    .course(course)
                    .chunkIndex(chunkIndex++)
                    .content(page.content.trim())
                    .pageNumber(page.pageNumber)
                    .tag(tag)
                    .createdAt(System.currentTimeMillis())
                    .build();
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    /**
     * 页面内容模型
     */
    public static class PageContent {
        public int pageNumber;
        public String content;

        public PageContent(int pageNumber, String content) {
            this.pageNumber = pageNumber;
            this.content = content;
        }
    }
}
