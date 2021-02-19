package org.bahmni_avni_integration.worker;

import org.bahmni_avni_integration.contract.avni.Encounter;
import org.bahmni_avni_integration.contract.avni.Subject;
import org.bahmni_avni_integration.contract.bahmni.OpenMRSPatient;
import org.bahmni_avni_integration.contract.internal.PatientToSubjectMetaData;
import org.bahmni_avni_integration.repository.avni.AvniEncounterRepository;
import org.bahmni_avni_integration.repository.openmrs.OpenMRSPatientRepository;
import org.bahmni_avni_integration.service.AvniEncounterService;
import org.bahmni_avni_integration.service.MappingMetaDataService;
import org.bahmni_avni_integration.service.PatientService;
import org.bahmni_avni_integration.service.SubjectService;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;

@Component
public class OpenMrsPatientEventWorker implements EventWorker {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OpenMRSPatientRepository patientRepository;

    @Autowired
    private MappingMetaDataService mappingMetaDataService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private AvniEncounterService avniEncounterService;

    @Override
    public void process(Event event) {
        OpenMRSPatient openMRSPatient = patientRepository.getPatient(event);
        logger.debug(String.format("Patient: name %s || uuid %s", openMRSPatient.getName(), openMRSPatient.getUuid()));
        PatientToSubjectMetaData patientToSubjectMetaData = mappingMetaDataService.getForPatientToSubject();
        Encounter encounter = avniEncounterService.getEncounter(openMRSPatient.getUuid(), patientToSubjectMetaData);
        if (encounter == null) {
            logger.debug("Enc not found");
            Subject subject = subjectService.findSubject(openMRSPatient, patientToSubjectMetaData);
            if (subject != null) {
                Encounter registrationEncounter = subjectService.createRegistrationEncounter(openMRSPatient, subject, patientToSubjectMetaData);
                logger.debug(String.format("New encounter created %s", registrationEncounter));
            } else {
                logger.debug("Subject not found");
            }
        } else {
            Encounter updatedEncounter = subjectService.updateRegistrationEncounter(encounter, openMRSPatient);
            logger.debug(String.format("Encounter updated %s", updatedEncounter));
        }
    }

    @Override
    public void cleanUp(Event event) {
    }
}
