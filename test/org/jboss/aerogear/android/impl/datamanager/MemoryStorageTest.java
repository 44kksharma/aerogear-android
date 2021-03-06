/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.android.impl.datamanager;

import org.jboss.aerogear.android.impl.datamanager.MemoryStorage;
import org.jboss.aerogear.android.impl.datamanager.DefaultStoreFactory;
import java.util.Collection;
import org.jboss.aerogear.android.ReadFilter;
import org.jboss.aerogear.android.datamanager.StoreType;
import static org.jboss.aerogear.android.impl.datamanager.StoreTypes.MEMORY;
import org.jboss.aerogear.android.impl.helper.Data;
import org.jboss.aerogear.android.impl.helper.DataWithNoIdConfigured;
import org.jboss.aerogear.android.impl.helper.DataWithNoPropertyId;
import org.jboss.aerogear.android.impl.reflection.PropertyNotFoundException;
import org.jboss.aerogear.android.impl.reflection.RecordIdNotFoundException;
import org.json.JSONException;
import org.json.JSONObject;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class MemoryStorageTest {

    private MemoryStorage<Data> store;
    private StubIdGenerator stubIdGenerator;

    @Before
    public void setup() {
        stubIdGenerator = new StubIdGenerator();
        store = new MemoryStorage<Data>(stubIdGenerator);
    }

    @Test
    public void testStoreType() {
        assertEquals("verifying the type", MEMORY, store.getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testStoreTypeThrowsException() {
        DefaultStoreFactory factory = new DefaultStoreFactory();
        StoreConfig config = new StoreConfig();
        config.setType(new FakeStoreType());
        factory.createStore(config);
    }

    @Test
    public void testReadAll() {
        store.save(new Data("foo", "desc of foo"));
        store.save(new Data("bar", "desc of bar"));

        Collection<Data> datas = store.readAll();
        assertNotNull("datas could not be null", datas);
        assertEquals("datas should 2 data", 2, datas.size());
    }

    @Test
    public void testReadWithFilter() {
        store.save(new Data("foo", "desc of foo"));
        store.save(new Data("bar", "desc of bar"));

        Collection<Data> datas = store.readWithFilter(new ReadFilter());
        assertNotNull("datas could not be null", datas);
        assertEquals("datas should 2 data", 2, datas.size());
    }

    @Test
    public void testReadWithFilterPerPage() {
        store.save(new Data("foo", "desc of foo"));
        store.save(new Data("bar", "desc of bar"));

        ReadFilter filter = new ReadFilter();
        filter.setLimit(1);

        Collection<Data> datas = store.readWithFilter(filter);
        assertNotNull("datas could not be null", datas);
        assertEquals("datas should 1 data", 1, datas.size());
        assertEquals("foo", datas.iterator().next().getName());

        filter.setOffset(1);
        datas = store.readWithFilter(filter);
        assertEquals("bar", datas.iterator().next().getName());
    }

    @Test
    public void testReadWithFilterWhere() throws JSONException {
        store.save(new Data("foo", "desc of foo"));
        store.save(new Data("bar", "desc of bar"));

        ReadFilter filter = new ReadFilter();
        filter.setWhere(new JSONObject("{\"name\":\"bar\"}"));

        Collection<Data> datas = store.readWithFilter(filter);

        assertNotNull("datas could not be null", datas);
        assertEquals("datas should 1 data", 1, datas.size());
        assertEquals("bar", datas.iterator().next().getName());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadWithFilterThrowsExceptionWithNestingJSON() throws JSONException {

        ReadFilter filter = new ReadFilter();
        filter.setWhere(new JSONObject("{\"name\":{\"name\":\"bar\"}}"));

        Collection<Data> datas = store.readWithFilter(filter);
    }

    @Test()
    public void testRead() {
        store.save(new Data("foo", "desc of foo"));
        Data data = store.read(1);
        assertNotNull("data could not be null", data);
    }

    @Test
    public void testSaveNoExistRecord() {
        Data data = new Data("foo", "desc of foo");
        store.save(data);
        assertEquals(Integer.valueOf(1), data.getId());
    }

    @Test
    public void testSaveExistRecord() {
        Data data = new Data("foo", "desc of foo");
        store.save(data);

        // Simulate sending the same id but not necessarily the same instance
        data = new Data(1, "bar", "desc of bar");
        store.save(data);

        data = store.read(1);

        assertEquals(Integer.valueOf(1), data.getId());
        assertEquals("bar", data.getName());
        assertEquals("desc of bar", data.getDescription());
    }

    @Test(expected = RecordIdNotFoundException.class)
    public void testSaveWithAnnotationNotConfigured() {
        MemoryStorage<DataWithNoIdConfigured> memoryStorage = new MemoryStorage<DataWithNoIdConfigured>(stubIdGenerator);
        memoryStorage.save(new DataWithNoIdConfigured());
    }

    @Test(expected = PropertyNotFoundException.class)
    public void testSaveWithNoPropertyToSetId() {
        MemoryStorage<DataWithNoPropertyId> memoryStorage = new MemoryStorage<DataWithNoPropertyId>(stubIdGenerator);
        memoryStorage.save(new DataWithNoPropertyId());
    }

    @Test
    public void testReset() {
        store.save(new Data("foo", "desc of foo"));
        store.save(new Data("bar", "desc of bar"));

        Data foo = store.read(1);
        assertNotNull("foo could not be null", foo);

        Data bar = store.read(2);
        assertNotNull("bar could not be null", bar);

        store.reset();

        foo = store.read(1);
        assertNull("foo should be null", foo);

        bar = store.read(2);
        assertNull("bar should be null", bar);
    }

    @Test
    public void testRemove() {
        store.save(new Data("foo", "desc of foo"));
        store.save(new Data("bar", "desc of bar"));

        Data foo = store.read(1);
        assertNotNull("foo could not be null", foo);

        Data bar = store.read(2);
        assertNotNull("bar could not be null", bar);

        store.remove(2);

        foo = store.read(1);
        assertNotNull("foo could not be null", foo);

        bar = store.read(2);
        assertNull("bar should be null", bar);
    }

    @Test
    public void testIsEmpty() {
        assertTrue("should be empty", store.isEmpty());
    }

    @Test
    public void testIsNotEmpty() {
        store.save(new Data("foo", "desc of foo"));
        assertFalse("should not be empty", store.isEmpty());
    }

    private static class FakeStoreType implements StoreType {

        @Override
        public String getName() {
            return "FAKE";
        }
    }

}
