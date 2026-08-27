/*
 *     Highly configurable PaperDoll mod. Forked from Extra Player Renderer.
 *     Copyright (C) 2024-2025  LucunJi(Original author), HappyRespawnanchor
 *
 *     This file is part of Ayame PaperDoll.
 *
 *     Ayame PaperDoll is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Ayame PaperDoll is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with Ayame PaperDoll.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.ayamemc.ayamepaperdoll.hud;


import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class DataBackup<T> {
    private final T target;
    private final List<DataBackupEntry<T, ?>> entries;
    private final Object[] values;

    public DataBackup(T target, List<DataBackupEntry<T, ?>> entries) {
        this.target = target;
        this.entries = entries;
        this.values = new Object[entries.size()];
    }

    public final void save() {
        for (int i = 0; i < this.entries.size(); i++) {
            this.values[i] = this.entries.get(i).saver.apply(this.target);
        }
    }

    @SuppressWarnings("unchecked")
    public final void restore() {
        for (int i = 0; i < this.entries.size(); i++) {
            DataBackupEntry<T, ?> entry = this.entries.get(i);
            ((BiConsumer<Object, Object>) entry.restorer).accept(this.target, this.values[i]);
        }
    }


    public static class DataBackupEntry<U, V> {
        private final Function<U, V> saver;
        private final BiConsumer<U, V> restorer;

        public DataBackupEntry(Function<U, V> saver, BiConsumer<U, V> restorer) {
            this.saver = saver;
            this.restorer = restorer;
        }
    }
}