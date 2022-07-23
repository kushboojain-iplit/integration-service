package org.avni_integration_service.goonj.worker.avni;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniEncounterRepository;
import org.avni_integration_service.avni.repository.AvniIgnoredConceptsRepository;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.goonj.GoonjEntityType;
import org.avni_integration_service.goonj.GoonjMappingGroup;
import org.avni_integration_service.goonj.repository.ActivityRepository;
import org.avni_integration_service.goonj.service.AvniGoonjErrorService;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActivityWorker extends GeneralEncounterWorker {
    private final ActivityRepository activityRepository;
    @Autowired
    public ActivityWorker(AvniEncounterRepository avniEncounterRepository,
                          AvniSubjectRepository avniSubjectRepository,
                          AvniIgnoredConceptsRepository avniIgnoredConceptsRepository,
                          AvniGoonjErrorService avniGoonjErrorService,
                          IntegratingEntityStatusRepository integrationEntityStatusRepository,
                          GoonjMappingGroup goonjMappingGroup,
                          ActivityRepository activityRepository) {
        super(avniEncounterRepository, avniSubjectRepository, avniIgnoredConceptsRepository,
                avniGoonjErrorService, goonjMappingGroup, integrationEntityStatusRepository,
                GoonjEntityType.Activity.getDbName(), Logger.getLogger(ActivityWorker.class));
        this.activityRepository = activityRepository;
    }
    public void process() {
        processEncounters();
    }
    @Override
    protected void createOrUpdateGeneralEncounter(GeneralEncounter generalEncounter, Subject subject) {
        processActivityEvent(generalEncounter, subject);
    }
    private void processActivityEvent(GeneralEncounter generalEncounter, Subject subject) {
        syncEncounterToGoonj(subject, generalEncounter, activityRepository, "ActivityId");
    }
}