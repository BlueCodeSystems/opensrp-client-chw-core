package org.smartregister.chw.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.R;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.util.Constants;
import org.smartregister.chw.anc.util.DBConstants;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.activity.CoreAncMemberProfileActivity;
import org.smartregister.chw.core.activity.CoreAncRegisterActivity;
import org.smartregister.chw.core.application.CoreChwApplication;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.presenter.CoreAncMemberProfilePresenter;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.custom_view.AncFloatingMenu;
import org.smartregister.chw.interactor.AncMemberProfileInteractor;
import org.smartregister.chw.model.FamilyProfileModel;
import org.smartregister.chw.schedulers.ChwScheduleTaskExecutor;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.commonregistry.AllCommonsRepository;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.Task;
import org.smartregister.family.domain.FamilyEventClient;
import org.smartregister.family.interactor.FamilyProfileInteractor;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.AllSharedPreferences;

import java.util.Date;
import java.util.Set;

import timber.log.Timber;

public class AncMemberProfileActivity extends CoreAncMemberProfileActivity {

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, AncMemberProfileActivity.class);
        intent.putExtra(Constants.ANC_MEMBER_OBJECTS.BASE_ENTITY_ID, baseEntityID);
        activity.startActivity(intent);
    }

    private void checkPhoneNumberProvided() {
        ((AncFloatingMenu) baseAncFloatingMenu).redraw(!StringUtils.isBlank(memberObject.getPhoneNumber())
                || !StringUtils.isBlank(getFamilyHeadPhoneNumber()));
    }


    @Override
    protected void registerPresenter() {
        presenter = new CoreAncMemberProfilePresenter(this, new AncMemberProfileInteractor(this), memberObject);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == org.smartregister.chw.core.R.id.action_remove_member) {
            CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);

            final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
            final CommonPersonObjectClient client =
                    new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
            client.setColumnmaps(commonPersonObject.getColumnmaps());

            IndividualProfileRemoveActivity.startIndividualProfileActivity(AncMemberProfileActivity.this, client, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyHead(), memberObject.getPrimaryCareGiver(), CoreAncRegisterActivity.class.getCanonicalName());
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_pregnancy_out_come) {
            PncRegisterActivity.startAncRegistrationActivity(AncMemberProfileActivity.this, memberObject.getBaseEntityId(), null, CoreConstants.JSON_FORM.getPregnancyOutcome(), AncLibrary.getInstance().getUniqueIdRepository().getNextUniqueId().getOpenmrsId(), memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.findItem(R.id.anc_danger_signs_outcome).setVisible(false);
        menu.findItem(R.id.action_malaria_diagnosis).setVisible(false);
        return true;
    }

    @Override // to chw
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON) {
            try {
                String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);
                if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(Utils.metadata().familyMemberRegister.updateEventType)) {
                    FamilyEventClient familyEventClient =
                            new FamilyProfileModel(memberObject.getFamilyName()).processUpdateMemberRegistration(jsonString, memberObject.getBaseEntityId());
                    new FamilyProfileInteractor().saveRegistration(familyEventClient, jsonString, true, ancMemberProfilePresenter());
                } else if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(CoreConstants.EventType.UPDATE_ANC_REGISTRATION)) {
                    AllSharedPreferences allSharedPreferences = org.smartregister.util.Utils.getAllSharedPreferences();
                    Event baseEvent = org.smartregister.chw.anc.util.JsonFormUtils.processJsonForm(allSharedPreferences, jsonString, Constants.TABLES.ANC_MEMBERS);
                    NCUtils.processEvent(baseEvent.getBaseEntityId(), new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(baseEvent)));
                    AllCommonsRepository commonsRepository = CoreChwApplication.getInstance().getAllCommonsRepository(CoreConstants.TABLE_NAME.ANC_MEMBER);

                    JSONArray field = org.smartregister.util.JsonFormUtils.fields(form);
                    JSONObject phoneNumberObject = org.smartregister.util.JsonFormUtils.getFieldJSONObject(field, DBConstants.KEY.PHONE_NUMBER);
                    String phoneNumber = phoneNumberObject.getString(CoreJsonFormUtils.VALUE);
                    String baseEntityId = baseEvent.getBaseEntityId();
                    if (commonsRepository != null) {
                        ContentValues values = new ContentValues();
                        values.put(DBConstants.KEY.PHONE_NUMBER, phoneNumber);
                        CoreChwApplication.getInstance().getRepository().getWritableDatabase().update(CoreConstants.TABLE_NAME.ANC_MEMBER, values, DBConstants.KEY.BASE_ENTITY_ID + " = ?  ", new String[]{baseEntityId});
                    }

                } else if (form.getString(JsonFormUtils.ENCOUNTER_TYPE).equals(CoreConstants.EventType.ANC_REFERRAL)) {
                    ancMemberProfilePresenter().createReferralEvent(Utils.getAllSharedPreferences(), jsonString);
                    showToast(this.getString(R.string.referral_submitted));
                }

            } catch (Exception e) {
                Timber.e(e, "AncMemberProfileActivity -- > onActivityResult");
            }
        } else if (requestCode == CoreConstants.ProfileActivityResults.CHANGE_COMPLETED) {
            ChwScheduleTaskExecutor.getInstance().execute(memberObject.getBaseEntityId(), CoreConstants.EventType.ANC_HOME_VISIT, new Date());
            finish();
        }
    }

    @Override
    protected void onResumption() {
        super.onResumption();
    }

    @Override
    public void startFormForEdit(Integer title_resource, String formName) {
        try {
            JSONObject form = org.smartregister.chw.util.JsonFormUtils.getAncPncForm(title_resource, formName, memberObject, this);
            startActivityForResult(org.smartregister.chw.util.JsonFormUtils.getAncPncStartFormIntent(form, this), JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openMedicalHistory() {
        AncMedicalHistoryActivity.startMe(this, memberObject);
    }

    @Override
    public void openUpcomingService() {
        AncUpcomingServicesActivity.startMe(this, memberObject);
    }

    @Override
    public void openFamilyDueServices() {
        Intent intent = new Intent(this, FamilyProfileActivity.class);

        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_BASE_ENTITY_ID, memberObject.getFamilyBaseEntityId());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_HEAD, memberObject.getFamilyHead());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.PRIMARY_CAREGIVER, memberObject.getPrimaryCareGiver());
        intent.putExtra(org.smartregister.family.util.Constants.INTENT_KEY.FAMILY_NAME, memberObject.getFamilyName());

        intent.putExtra(CoreConstants.INTENT_KEY.SERVICE_DUE, true);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        super.onClick(view);
        int id = view.getId();
        if (id == R.id.textview_record_visit || id == R.id.textview_record_reccuring_visit) {
            AncHomeVisitActivity.startMe(this, memberObject.getBaseEntityId(), false);
        } else if (id == R.id.textview_edit) {
            AncHomeVisitActivity.startMe(this, memberObject.getBaseEntityId(), true);
        }
    }

    @Override
    public void setClientTasks(Set<Task> taskList) {
        //overridden
    }

    @Override
    public void startFormActivity(JSONObject formJson) {
        startActivityForResult(CoreJsonFormUtils.getJsonIntent(this, formJson, Utils.metadata().familyMemberFormActivity),
                JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void initializeFloatingMenu() {
        baseAncFloatingMenu = new AncFloatingMenu(this, getAncWomanName(),
                memberObject.getPhoneNumber(), getFamilyHeadName(), getFamilyHeadPhoneNumber(), getProfileType());

        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.anc_fab:
                    checkPhoneNumberProvided();
                    ((AncFloatingMenu) baseAncFloatingMenu).animateFAB();
                    break;
                case R.id.call_layout:
                    ((AncFloatingMenu) baseAncFloatingMenu).launchCallWidget();
                    ((AncFloatingMenu) baseAncFloatingMenu).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    ancMemberProfilePresenter().startAncReferralForm();
                    ((AncFloatingMenu) baseAncFloatingMenu).animateFAB();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }

        };

        ((AncFloatingMenu) baseAncFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
        baseAncFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseAncFloatingMenu, linearLayoutParams);
    }
}
