/*
  Copyright (c) Microsoft Corporation. All rights reserved.
  Licensed under the MIT License. See License.txt in the project root for
  license information.
 */

package com.microsoft.azure.toolkit.lib.common.model;

import lombok.Getter;

import javax.annotation.Nonnull;
import java.io.File;

public interface IArtifact {
    String getId();

    @Nonnull
    static FileArtifact fromFile(@Nonnull final File file) {
        return new FileArtifact(file);
    }

    @Nonnull
    static IdArtifact fromId(@Nonnull final String id) {
        return new IdArtifact(id);
    }

    @Getter
    class FileArtifact implements IArtifact {

        private final File file;

        public FileArtifact(@Nonnull final File file) {
            this.file = file;
        }

        @Override
        public String getId() {
            return file.getAbsolutePath();
        }
    }

    @Getter
    class IdArtifact implements IArtifact {
        private final String id;

        public IdArtifact(@Nonnull final String id) {
            this.id = id;
        }
    }
}

