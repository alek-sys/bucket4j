/*
 *
 * Copyright 2015-2018 Vladimir Bukhtoyarov
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package io.github.bucket4j.grid.infinispan;

import io.github.bucket4j.grid.CommandResult;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.jcache.JCacheEntryProcessor;
import io.github.bucket4j.serialization.DeserializationAdapter;
import io.github.bucket4j.serialization.SerializationAdapter;
import io.github.bucket4j.serialization.SerializationHandle;
import org.infinispan.functional.EntryView;
import org.infinispan.util.function.SerializableFunction;

import javax.cache.processor.MutableEntry;
import java.io.IOException;
import java.io.Serializable;

public class SerializableFunctionAdapter<K extends Serializable, R extends Serializable> implements SerializableFunction<EntryView.ReadWriteEntryView<K, GridBucketState>, CommandResult<R>> {

    private static final long serialVersionUID = 42L;

    private final JCacheEntryProcessor<K, R> entryProcessor;

    public SerializableFunctionAdapter(JCacheEntryProcessor<K, R> entryProcessor) {
        this.entryProcessor = entryProcessor;
    }

    public JCacheEntryProcessor<K, R> getEntryProcessor() {
        return entryProcessor;
    }

    public static SerializationHandle<SerializableFunctionAdapter> SERIALIZATION_HANDLE = new SerializationHandle<SerializableFunctionAdapter>() {

        @Override
        public <I> SerializableFunctionAdapter deserialize(DeserializationAdapter<I> adapter, I input) throws IOException {
            JCacheEntryProcessor entryProcessor = (JCacheEntryProcessor) adapter.readObject(input);
            return new SerializableFunctionAdapter(entryProcessor);
        }

        @Override
        public <O> void serialize(SerializationAdapter<O> adapter, O output, SerializableFunctionAdapter serializableObject) throws IOException {
            adapter.writeObject(output, serializableObject.entryProcessor);
        }

        @Override
        public int getTypeId() {
            return 21;
        }

        @Override
        public Class<SerializableFunctionAdapter> getSerializedType() {
            return SerializableFunctionAdapter.class;
        }
    };

    @Override
    public CommandResult<R> apply(EntryView.ReadWriteEntryView<K, GridBucketState> entryView) {
        return entryProcessor.process(new MutableEntryAdapter<>(entryView));
    }

    private static class MutableEntryAdapter<K extends Serializable> implements MutableEntry<K, GridBucketState> {

        private final EntryView.ReadWriteEntryView<K, GridBucketState> entryView;

        public MutableEntryAdapter(EntryView.ReadWriteEntryView<K, GridBucketState> entryView) {
            this.entryView = entryView;
        }

        @Override
        public K getKey() {
            return entryView.key();
        }

        @Override
        public GridBucketState getValue() {
            GridBucketState sourceState = entryView.get();
            return sourceState.deepCopy();
        }

        @Override
        public boolean exists() {
            return entryView.find().isPresent();
        }

        @Override
        public void remove() {
            entryView.remove();
        }

        @Override
        public void setValue(GridBucketState value) {
            entryView.set(value);
        }

        @Override
        public <T> T unwrap(Class<T> clazz) {
            throw new UnsupportedOperationException();
        }

    }

}
