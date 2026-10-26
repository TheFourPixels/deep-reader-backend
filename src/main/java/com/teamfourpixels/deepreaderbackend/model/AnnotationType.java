package com.teamfourpixels.deepreaderbackend.model;

public enum AnnotationType {
    SLANG("Сленг / Жаргонизм"),
    METAPHOR("Авторская метафора"),
    IDIOM("Идиома / Фразеологизм"),
    CULTURAL_CONTEXT("Культурный контекст"),
    ALLUSION("Аллюзия / Подтекст");

    private final String displayName;

    AnnotationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
