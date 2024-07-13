package com.rocketseat.planner.trip;

import com.rocketseat.planner.activity.ActivityData;
import com.rocketseat.planner.activity.ActivityRequest;
import com.rocketseat.planner.activity.ActivityResponse;
import com.rocketseat.planner.activity.ActivityService;
import com.rocketseat.planner.link.LinkData;
import com.rocketseat.planner.link.LinkRequest;
import com.rocketseat.planner.link.LinkResponse;
import com.rocketseat.planner.link.LinkService;
import com.rocketseat.planner.participant.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Tag(name = "Trips", description = "Endpoints das Viagens")
@RestController
@RequestMapping("/trips")
public class TripController {
    @Autowired
    private LinkService linkService;
    @Autowired
    private ParticipantService participantService;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private TripRepository repository;

    //TRIPS
    @PostMapping
    public ResponseEntity<String> createTrip(@RequestBody TripRequest payload) {
        try {
            LocalDateTime dateStart = LocalDateTime.parse(payload.starts_at(), DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime dateEnd = LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME);
            if (dateStart.isBefore(dateEnd)) {
                Trip newTrip = new Trip(payload);
                this.repository.save(newTrip);
                this.participantService.registerParticipantsToEvent(payload.emails_to_invite(), newTrip);
                return ResponseEntity.ok("Successfully registered!" + new TripCreateResponse(newTrip.getId()));
            } else return ResponseEntity.badRequest().body("Error: Data de término deve ser Posterior a de Inicio");
        } catch (DateTimeException e) {
            return ResponseEntity.badRequest().body("Error: Data fora do Formato válido(ISO 8601)");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripDetails(@PathVariable UUID id) {
        Optional<Trip> trip = this.repository.findById(id);
        return trip.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable UUID id, @RequestBody TripRequest payload) {
        Optional<Trip> trip = this.repository.findById(id);
        if (trip.isPresent()) {
            Trip rawTrip = trip.get();
            rawTrip.setEndsAt(LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setStartsAt(LocalDateTime.parse(payload.starts_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setDestination(payload.destination());
            this.repository.save(rawTrip);
            return ResponseEntity.ok(rawTrip);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/confirm")
    public ResponseEntity<Trip> confirmTrip(@PathVariable UUID id) {
        Optional<Trip> trip = this.repository.findById(id);
        if (trip.isPresent()) {
            Trip rawTrip = trip.get();
            rawTrip.setIsConfirmed(true);
            this.participantService.triggerConfirmationEmailToParticipants(id);
            this.repository.save(rawTrip);
            return ResponseEntity.ok(rawTrip);
        }
        return ResponseEntity.notFound().build();
    }

    //PARTICIPANTS
    @PostMapping("/{id}/invite")
    public ResponseEntity<ParticipantCreateResponse> inviteParticipant(@PathVariable UUID id, @RequestBody ParticipantRequest payload) {
        Optional<Trip> trip = this.repository.findById(id);
        if (trip.isPresent()) {
            Trip rawTrip = trip.get();

            ParticipantCreateResponse participantResponse = this.participantService.registerParticipant(payload.email(), rawTrip);
            if (rawTrip.getIsConfirmed())
                this.participantService.triggerConfirmationEmailToParticipant(payload.email());
            return ResponseEntity.ok(participantResponse);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantData>> getAllParticipants(@PathVariable UUID id) {
        List<ParticipantData> participantsList = this.participantService.getAllParticipantsFromEvent(id);
        return ResponseEntity.ok(participantsList);
    }

    //ACTIVITIES
    @PostMapping("/{id}/activities")
    public ResponseEntity<String> registerActivity(@PathVariable UUID id, @RequestBody ActivityRequest payload) {
        try {
            Optional<Trip> trip = this.repository.findById(id);
            LocalDateTime dataActivity = LocalDateTime.parse(payload.occurs_at(), DateTimeFormatter.ISO_DATE_TIME);
            if (trip.isPresent()) {
                LocalDateTime dataStartTrip = trip.get().getStartsAt();
                LocalDateTime dataEndTrip = trip.get().getEndsAt();
                Trip rawTrip = trip.get();
                if (dataActivity.isEqual(dataStartTrip) || dataActivity.isEqual(dataEndTrip) ||
                        dataActivity.isAfter(dataStartTrip) && dataActivity.isBefore(dataEndTrip)) {
                    ActivityResponse activityResponse = this.activityService.saveActivity(payload, rawTrip);
                    return ResponseEntity.ok("Successfully Registered!" + activityResponse);
                } else
                    return ResponseEntity.badRequest().body("Error: Data da atividade deve estar entre a data de inicio e fim da Viagem");

            }
            return ResponseEntity.notFound().build();


        } catch (DateTimeException e) {
            return ResponseEntity.badRequest().body("Error: Data fora do formato válido(ISO 8601)");
        }


    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<String> getAllActivities(@PathVariable UUID id) {
            List<ActivityData> activitiesList = this.activityService.getAllActivitesFromId(id);
            if (activitiesList.isEmpty()){
                return ResponseEntity.badRequest().body("Lista Vazia "+activitiesList);
            }
            return ResponseEntity.ok(""+activitiesList);



    }

    //LINKS
    @PostMapping("/{id}/links")
    public ResponseEntity<LinkResponse> registerLink(@PathVariable UUID id, @RequestBody LinkRequest payload) {
        Optional<Trip> trip = this.repository.findById(id);
        if (trip.isPresent()) {
            Trip rawTrip = trip.get();
            LinkResponse linkResponse = this.linkService.createLink(payload, rawTrip);

            return ResponseEntity.ok(linkResponse);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/links")
    public ResponseEntity<List<LinkData>> getAllLinks(@PathVariable UUID id) {
        List<LinkData> linksList = this.linkService.getAllLinksFromId(id);
        return ResponseEntity.ok(linksList);
    }

}
