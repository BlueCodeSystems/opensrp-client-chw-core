package org.smartregister.chw.core.application;

import org.mockito.Mockito;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.configurableviews.ConfigurableViewsLibrary;
import org.smartregister.family.BuildConfig;
import org.smartregister.family.FamilyLibrary;
import org.smartregister.family.domain.FamilyMetadata;
import org.smartregister.repository.Repository;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;

/**
 * @author rkodev
 */
public class TestApplication extends CoreChwApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        context = Context.getInstance();
        context.updateApplicationContext(getApplicationContext());
        CoreLibrary.init(context);
        ConfigurableViewsLibrary.init(context);

        FamilyLibrary.init(context, getMetadata(), BuildConfig.VERSION_CODE, 2);

        setTheme(org.smartregister.family.R.style.FamilyTheme_NoActionBar);
    }

    @Override
    public FamilyMetadata getMetadata() {
        return Mockito.mock(FamilyMetadata.class);
    }

    @Override
    public ArrayList<String> getAllowedLocationLevels() {
        return new ArrayList<>();
    }

    @Override
    public ArrayList<String> getFacilityHierarchy() {
        return new ArrayList<>();
    }

    @Override
    public String getDefaultLocationLevel() {
        return "default";
    }


    @Override
    public Repository getRepository() {
        repository = mock(Repository.class);
        return repository;
    }
}
