/*
 *  Copyright (C) 2013 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.async.loader;


import android.content.Context;
import android.database.Cursor;

import org.akvo.flow.async.loader.base.DataLoader;
import org.akvo.flow.dao.SurveyDbAdapter;
import org.akvo.flow.domain.SurveyGroup;

public class SurveyGroupLoader extends DataLoader<Cursor> {
    private long mSurveyGroupId;
    
    public SurveyGroupLoader(Context context, SurveyDbAdapter db) {
        this(context, db, SurveyGroup.ID_NONE);
    }
    
    public SurveyGroupLoader(Context context, SurveyDbAdapter db, long surveyGroupId) {
        super(context, db);
        mSurveyGroupId = surveyGroupId;
    }

    @Override
    protected Cursor loadData(SurveyDbAdapter database) {
        if (SurveyGroup.ID_NONE == mSurveyGroupId) {
            return database.getSurveyGroup(mSurveyGroupId);
        }
        // Load all
        return database.getSurveyGroups();
    }

}
