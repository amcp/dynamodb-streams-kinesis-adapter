/*
 * Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.services.dynamodbv2.streamsadapter.util;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.kinesis.model.Record;

public class CountingRecordProcessor implements IRecordProcessor {

    private static final Log LOG = LogFactory.getLog(CountingRecordProcessor.class);

    private RecordProcessorTracker tracker;

    private String shardId;
    private Integer checkpointCounter;
    private Integer recordCounter;

    CountingRecordProcessor(RecordProcessorTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public void initialize(InitializationInput initializationInput) {
        this.shardId = initializationInput.getShardId();
        checkpointCounter = 0;
        recordCounter = 0;
    }

    @Override
    public void processRecords(ProcessRecordsInput processRecordsInput) {
        for(Record record : processRecordsInput.getRecords()) {
            recordCounter += 1;
            checkpointCounter += 1;
            if(checkpointCounter % 10 == 0) {
                try {
                    processRecordsInput.getCheckpointer().checkpoint(record.getSequenceNumber());
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void shutdown(ShutdownInput shutdownInput) {
        if(shutdownInput.getShutdownReason() == ShutdownReason.TERMINATE) {
            try {
                shutdownInput.getCheckpointer().checkpoint();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LOG.info("Processed "+ recordCounter + " records for " + shardId);
        tracker.shardProcessed(shardId, recordCounter);
    }

}
