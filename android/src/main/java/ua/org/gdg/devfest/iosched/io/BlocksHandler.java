/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ua.org.gdg.devfest.iosched.io;

import ua.org.gdg.devfest.iosched.io.model.Day;
import ua.org.gdg.devfest.iosched.io.model.EventSlots;
import ua.org.gdg.devfest.iosched.io.model.TimeSlot;
import ua.org.gdg.devfest.iosched.provider.ScheduleContract;
import ua.org.gdg.devfest.iosched.provider.ScheduleContract.Blocks;
import ua.org.gdg.devfest.iosched.util.Lists;
import ua.org.gdg.devfest.iosched.util.ParserUtils;
import ua.org.gdg.devfest.iosched.util.UIUtils;

import android.content.ContentProviderOperation;
import android.content.Context;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import static ua.org.gdg.devfest.iosched.util.LogUtils.LOGE;
import static ua.org.gdg.devfest.iosched.util.LogUtils.LOGV;
import static ua.org.gdg.devfest.iosched.util.LogUtils.makeLogTag;


public class BlocksHandler extends JSONHandler {

    private static final String TAG = makeLogTag(BlocksHandler.class);

    public BlocksHandler(Context context) {
        super(context);
    }

    public ArrayList<ContentProviderOperation> parse(String json) throws IOException {
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        try {
            EventSlots eventSlots = new EventSlots();
            eventSlots.fromJSON(new JSONObject(json));
            for(Day day : eventSlots.getDays()) {
                String date = day.date;
                for(TimeSlot timeSlot: day.getSlots()) {
                    parseSlot(date, timeSlot, batch);
                }
            }
        } catch (Throwable e) {
            LOGE(TAG, e.toString());
        }
        return batch;
    }

    private static void parseSlot(String date, TimeSlot slot,
            ArrayList<ContentProviderOperation> batch) {
        ContentProviderOperation.Builder builder = ContentProviderOperation
                .newInsert(ScheduleContract.addCallerIsSyncAdapterParameter(Blocks.CONTENT_URI));
        //LOGD(TAG, "Inside parseSlot:" + date + ",  " + slot);
        String start = slot.start;
        String end = slot.end;

        String type = Blocks.BLOCK_TYPE_GENERIC;
        if (slot.type != null) {
            type = slot.type;
        }
        String title = "N_D";
        if (slot.title != null) {
            title = slot.title;
        }
        String startTime = date + "T" + start + ":00.000" + UIUtils.CONFERENCE_TIME_ZONE_STRING;
        String endTime = date + "T" + end + ":00.000" + UIUtils.CONFERENCE_TIME_ZONE_STRING;
        LOGV(TAG, "startTime:" + startTime);
        long startTimeL = ParserUtils.parseTime(startTime);
        long endTimeL = ParserUtils.parseTime(endTime);
        final String blockId = Blocks.generateBlockId(startTimeL, endTimeL);
        LOGV(TAG, "blockId:" + blockId);
        LOGV(TAG, "title:" + title);
        LOGV(TAG, "start:" + startTimeL);
        builder.withValue(Blocks.BLOCK_ID, blockId);
        builder.withValue(Blocks.BLOCK_TITLE, title);
        builder.withValue(Blocks.BLOCK_START, startTimeL);
        builder.withValue(Blocks.BLOCK_END, endTimeL);
        builder.withValue(Blocks.BLOCK_TYPE, type);
        builder.withValue(Blocks.BLOCK_META, slot.meta);
        batch.add(builder.build());
    }
}