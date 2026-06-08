package com.ai.aicodehelper.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RagConfig {

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Value("${ai.rag.docs.path}")
    private String docsPath;

    @Bean
    public ContentRetriever contentRetriever() {

        //1. 加载文档
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(docsPath);

        //2. 文档切割：每隔文档安装段落切割，最大1000字符，每次最多重叠200个字符
        DocumentByParagraphSplitter documentByParagraphSplitter =
                new DocumentByParagraphSplitter(1000, 200);

        //3. 自定义文档加载器，把文档转成向量并保存到向量数据库中
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentByParagraphSplitter)
                // 提高文档质量,为每个切割后的段落添加文件名作为元信息
                .textSegmentTransformer(textSegment -> TextSegment.from(textSegment.metadata().getString("file_name") +
                        "\n" + textSegment.text(), textSegment.metadata()))
                // 创建向量模型
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        // 加载文档
        ingestor.ingest(documents);

        //4. 自定义内容加载器
        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore)
                .maxResults(5) // 返回5个最相关的结果
                .minScore(0.75) // 过滤掉分数低于0.75的向量
                .build();
        return contentRetriever;

    }


}
