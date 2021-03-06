// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.task;

import org.apache.doris.catalog.Column;
import org.apache.doris.thrift.TAlterTabletReq;
import org.apache.doris.thrift.TColumn;
import org.apache.doris.thrift.TCreateTabletReq;
import org.apache.doris.thrift.TKeysType;
import org.apache.doris.thrift.TResourceInfo;
import org.apache.doris.thrift.TStorageType;
import org.apache.doris.thrift.TTabletSchema;
import org.apache.doris.thrift.TTaskType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Deprecated
public class SchemaChangeTask extends AgentTask {

    private long baseReplicaId;
    private int baseSchemaHash;
    private TStorageType storageType;
    private TKeysType keysType;

    private int newSchemaHash;
    private short newShortKeyColumnCount;
    private List<Column> newColumns;

    // bloom filter columns
    private Set<String> bfColumns;
    private double bfFpp;

    public SchemaChangeTask(TResourceInfo resourceInfo, long backendId, long dbId, long tableId,
                            long partitionId, long indexId, long baseTabletId, long baseReplicaId,
                            List<Column> newColumns, int newSchemaHash, int baseSchemaHash,
                            short newShortKeyColumnCount, TStorageType storageType,
                            Set<String> bfColumns, double bfFpp, TKeysType keysType) {
        super(resourceInfo, backendId, TTaskType.SCHEMA_CHANGE, dbId, tableId, partitionId, indexId, baseTabletId);

        this.baseReplicaId = baseReplicaId;
        this.baseSchemaHash = baseSchemaHash;
        this.storageType = storageType;
        this.keysType = keysType;

        this.newSchemaHash = newSchemaHash;
        this.newShortKeyColumnCount = newShortKeyColumnCount;
        this.newColumns = newColumns;

        this.bfColumns = bfColumns;
        this.bfFpp = bfFpp;
    }

    public TAlterTabletReq toThrift() {
        TAlterTabletReq tAlterTabletReq = new TAlterTabletReq();

        tAlterTabletReq.setBaseTabletId(tabletId);
        tAlterTabletReq.setBaseSchemaHash(baseSchemaHash);

        // make 1 TCreateTableReq
        TCreateTabletReq createTabletReq = new TCreateTabletReq();
        createTabletReq.setTabletId(tabletId);

        // no need to set version
        // schema
        TTabletSchema tSchema = new TTabletSchema();
        tSchema.setShortKeyColumnCount(newShortKeyColumnCount);
        tSchema.setSchemaHash(newSchemaHash);
        tSchema.setStorageType(storageType);
        tSchema.setKeysType(keysType);
        int deleteSign = -1;
        List<TColumn> tColumns = new ArrayList<TColumn>();
        for (int i = 0; i < newColumns.size(); i++) {
            Column column = newColumns.get(i);
            TColumn tColumn = column.toThrift();
            // is bloom filter column
            if (bfColumns != null && bfColumns.contains(column.getName())) {
                tColumn.setIsBloomFilterColumn(true);
            }
            tColumn.setVisible(column.isVisible());
            if (column.isDeleteSignColumn()) {
                deleteSign = i;
            }
            tColumns.add(tColumn);
        }
        tSchema.setColumns(tColumns);
        tSchema.setDeleteSignIdx(deleteSign);

        if (bfColumns != null) {
            tSchema.setBloomFilterFpp(bfFpp);
        }
        createTabletReq.setTabletSchema(tSchema);
        createTabletReq.setTableId(tableId);
        createTabletReq.setPartitionId(partitionId);

        tAlterTabletReq.setNewTabletReq(createTabletReq);

        return tAlterTabletReq;
    }

    public long getReplicaId() {
        return baseReplicaId;
    }

    public int getSchemaHash() {
        return newSchemaHash;
    }

    public int getBaseSchemaHash() {
        return baseSchemaHash;
    }

    public short getNewShortKeyColumnCount() {
        return newShortKeyColumnCount;
    }

    public TStorageType getStorageType() {
        return storageType;
    }

    public List<Column> getColumns() {
        return newColumns;
    }

}
