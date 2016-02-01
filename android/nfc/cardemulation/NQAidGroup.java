/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 *
 * Copyright (C) 2015 NXP Semiconductors
 * The original Work has been changed by NXP Semiconductors.
 * Copyright (C) 2015 The Android Open Source Project
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

package android.nfc.cardemulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.lang.Class;
import java.lang.reflect.Field;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * The NQAidGroup class represents a group of Application Identifiers (AIDs).
 *
 * <p>The format of AIDs is defined in the ISO/IEC 7816-4 specification. This class
 * requires the AIDs to be input as a hexadecimal string, with an even amount of
 * hexadecimal characters, e.g. "F014811481".
 *
 * @hide
 */
public final class NQAidGroup extends AidGroup implements Parcelable {
    /**
     * The maximum number of AIDs that can be present in any one group.
     */
    public static final int MAX_NUM_AIDS = 256;

    static final String TAG = "NQAidGroup";


    /**
     * Creates a new NQAidGroup object.
     *
     * @param aids The list of AIDs present in the group
     * @param category The category of this group, e.g. {@link CardEmulation#CATEGORY_PAYMENT}
     */
    public NQAidGroup(List<String> aids, String category, String description) {
        super(aids, category);
        this.description = description;
    }

    /**
     * Creates a new NQAidGroup object.
     *
     * @param aids The list of AIDs present in the group
     * @param category The category of this group, e.g. {@link CardEmulation#CATEGORY_PAYMENT}
     */
    public NQAidGroup(List<String> aids, String category) {
        super(aids, category);
    }


    static String getDescription(AidGroup aid) {
        Field[] fs = aid.getClass().getDeclaredFields();
        for(Field f : fs) {
            f.setAccessible(true);
        }
        return aid.description;
    }

    public NQAidGroup(AidGroup aid) {
        this(aid.getAids(), aid.getCategory(), getDescription(aid));
    }

    public NQAidGroup(String category, String description) {
        super(category, description);
    }

    /**
     * @return the decription of this AID group
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("Category: " + category +
                  ", AIDs:");
        for (String aid : aids) {
            out.append(aid);
            out.append(", ");
        }
        return out.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(category);
        dest.writeInt(aids.size());
        if (aids.size() > 0) {
            dest.writeStringList(aids);
        }
        if(description != null) {
            dest.writeString(description);
        } else {
            dest.writeString(null);
        }
    }

    public static final Parcelable.Creator<NQAidGroup> CREATOR =
            new Parcelable.Creator<NQAidGroup>() {

        @Override
        public NQAidGroup createFromParcel(Parcel source) {
            String category = source.readString();
            int listSize = source.readInt();
            ArrayList<String> aidList = new ArrayList<String>();
            if (listSize > 0) {
                source.readStringList(aidList);
            }
            String description = source.readString();
            return new NQAidGroup(aidList, category, description);
        }

        @Override
        public NQAidGroup[] newArray(int size) {
            return new NQAidGroup[size];
        }
    };

    static public NQAidGroup createFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
        String category = null;
        String description = null;
        ArrayList<String> aids = new ArrayList<String>();
        NQAidGroup group = null;
        boolean inGroup = false;

        int eventType = parser.getEventType();
        int minDepth = parser.getDepth();
        while (eventType != XmlPullParser.END_DOCUMENT && parser.getDepth() >= minDepth) {
            String tagName = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if (tagName.equals("aid")) {
                    if (inGroup) {
                        String aid = parser.getAttributeValue(null, "value");
                        if (aid != null) {
                            aids.add(aid.toUpperCase());
                        }
                    } else {
                        Log.d(TAG, "Ignoring <aid> tag while not in group");
                    }
                } else if (tagName.equals("aid-group")) {
                    category = parser.getAttributeValue(null, "category");
                    description = parser.getAttributeValue(null, "description");
                    if (category == null) {
                        Log.e(TAG, "<aid-group> tag without valid category");
                        return null;
                    }
                    inGroup = true;
                } else {
                    Log.d(TAG, "Ignoring unexpected tag: " + tagName);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (tagName.equals("aid-group") && inGroup && aids.size() > 0) {
                    group = new NQAidGroup(aids, category, description);
                    break;
                }
            }
            eventType = parser.next();
        }
        return group;
    }

    public void writeAsXml(XmlSerializer out) throws IOException {
        out.startTag(null, "aid-group");
        out.attribute(null, "category", category);
        if(description != null)
            out.attribute(null, "description", description);
        for (String aid : aids) {
            out.startTag(null, "aid");
            out.attribute(null, "value", aid);
            out.endTag(null, "aid");
        }
        out.endTag(null, "aid-group");
    }

    static boolean isValidCategory(String category) {
        return CardEmulation.CATEGORY_PAYMENT.equals(category) ||
                CardEmulation.CATEGORY_OTHER.equals(category);
    }
}
