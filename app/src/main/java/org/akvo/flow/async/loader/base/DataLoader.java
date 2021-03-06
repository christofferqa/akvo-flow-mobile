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

package org.akvo.flow.async.loader.base;

import android.content.Context;

import org.akvo.flow.dao.SurveyDbAdapter;

public abstract class DataLoader<D> extends AsyncLoader<D> {
    private SurveyDbAdapter mDatabase;
    
    public DataLoader(Context context, SurveyDbAdapter db) {
        super(context);
        mDatabase = db;
    }

    protected abstract D loadData(SurveyDbAdapter database);

    @Override
    public D loadInBackground() {
        return loadData(mDatabase);
    }
    
}
