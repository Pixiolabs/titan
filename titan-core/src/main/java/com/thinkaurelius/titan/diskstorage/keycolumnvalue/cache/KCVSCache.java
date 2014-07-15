package com.thinkaurelius.titan.diskstorage.keycolumnvalue.cache;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.thinkaurelius.titan.diskstorage.BackendException;
import com.thinkaurelius.titan.diskstorage.Entry;
import com.thinkaurelius.titan.diskstorage.EntryList;
import com.thinkaurelius.titan.diskstorage.StaticBuffer;
import com.thinkaurelius.titan.diskstorage.keycolumnvalue.*;
import com.thinkaurelius.titan.diskstorage.util.CacheMetricsAction;
import com.thinkaurelius.titan.util.stats.MetricManager;

import java.util.List;
import java.util.Map;

/**
 * @author Matthias Broecheler (me@matthiasb.com)
 */
public abstract class KCVSCache implements KeyColumnValueStore {

    public static final List<Entry> NO_DELETIONS = ImmutableList.of();

    private final String metricsName;
    private final boolean validateKeysOnly = true;

    protected final KeyColumnValueStore store;

    protected KCVSCache(KeyColumnValueStore store, String metricsName) {
        this.metricsName = metricsName;
        Preconditions.checkNotNull(store);
        this.store = store;
    }

    protected boolean hasValidateKeysOnly() {
        return validateKeysOnly;
    }

    protected void incActionBy(int by, CacheMetricsAction action, StoreTransaction txh) {
        assert by>=1;
        if (metricsName!=null && txh.getConfiguration().hasGroupName()) {
            MetricManager.INSTANCE.getCounter(txh.getConfiguration().getGroupName(), metricsName, action.getName()).inc(by);
        }
    }

    public abstract void clearCache();

    protected abstract void invalidate(StaticBuffer key, List<CachableStaticBuffer> entries);

    @Override
    public void mutate(StaticBuffer key, List<Entry> additions, List<StaticBuffer> deletions, StoreTransaction txh) throws BackendException {
        throw new UnsupportedOperationException("Only supports mutateEntries()");
    }

    public void mutateEntries(StaticBuffer key, List<Entry> additions, List<Entry> deletions, StoreTransaction txh) throws BackendException {
        assert txh instanceof CacheTransaction;
        ((CacheTransaction) txh).mutate(this, key, additions, deletions);
    }

    //############### SIMPLE PROXY ###########

    protected final StoreTransaction getTx(StoreTransaction txh) {
        assert txh instanceof CacheTransaction;
        return ((CacheTransaction) txh).getWrappedTransaction();
    }

    public EntryList getSliceNoCache(KeySliceQuery query, StoreTransaction txh) throws BackendException {
        return store.getSlice(query,getTx(txh));
    }

    public Map<StaticBuffer, EntryList> getSliceNoCache(List<StaticBuffer> keys, SliceQuery query, StoreTransaction txh) throws BackendException {
        return store.getSlice(keys,query,getTx(txh));
    }

    @Override
    public void acquireLock(StaticBuffer key, StaticBuffer column, StaticBuffer expectedValue, StoreTransaction txh) throws BackendException {
        store.acquireLock(key, column, expectedValue, getTx(txh));
    }

    @Override
    public KeyIterator getKeys(KeyRangeQuery keyQuery, StoreTransaction txh) throws BackendException {
        return store.getKeys(keyQuery, getTx(txh));
    }

    @Override
    public KeyIterator getKeys(SliceQuery columnQuery, StoreTransaction txh) throws BackendException {
        return store.getKeys(columnQuery, getTx(txh));
    }

    @Override
    public String getName() {
        return store.getName();
    }

    @Override
    public void close() throws BackendException {
        store.close();
    }

}