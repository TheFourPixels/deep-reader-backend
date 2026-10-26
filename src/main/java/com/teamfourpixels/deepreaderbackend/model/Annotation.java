package com.teamfourpixels.deepreaderbackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "annotations")
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    @JsonIgnore
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    @JsonIgnore
    private Chapter chapter;

    @Column(name = "target_phrase", nullable = false, length = 512)
    private String targetPhrase;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "annotation_type", nullable = false, length = 50)
    private AnnotationType type = AnnotationType.SLANG;

    @Column(name = "context_snippet", columnDefinition = "TEXT")
    private String contextSnippet;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Annotation() {
    }

    public Annotation(Book book, Chapter chapter, String targetPhrase, String explanation, AnnotationType type, String contextSnippet) {
        this.book = book;
        this.chapter = chapter;
        this.targetPhrase = targetPhrase;
        this.explanation = explanation;
        this.type = type != null ? type : AnnotationType.SLANG;
        this.contextSnippet = contextSnippet;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public String getTargetPhrase() {
        return targetPhrase;
    }

    public void setTargetPhrase(String targetPhrase) {
        this.targetPhrase = targetPhrase;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public AnnotationType getType() {
        return type;
    }

    public void setType(AnnotationType type) {
        this.type = type;
    }

    public String getContextSnippet() {
        return contextSnippet;
    }

    public void setContextSnippet(String contextSnippet) {
        this.contextSnippet = contextSnippet;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
