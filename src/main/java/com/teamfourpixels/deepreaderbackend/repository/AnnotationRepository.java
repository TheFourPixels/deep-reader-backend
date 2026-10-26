package com.teamfourpixels.deepreaderbackend.repository;

import com.teamfourpixels.deepreaderbackend.model.Annotation;
import com.teamfourpixels.deepreaderbackend.model.AnnotationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnotationRepository extends JpaRepository<Annotation, Long> {

    List<Annotation> findByBookId(Long bookId);

    List<Annotation> findByChapterId(Long chapterId);

    List<Annotation> findByBookIdAndType(Long bookId, AnnotationType type);

    long countByType(AnnotationType type);

    long countByBookId(Long bookId);

    long countByChapterId(Long chapterId);
}
