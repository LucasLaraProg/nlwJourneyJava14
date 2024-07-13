package com.rocketseat.planner.participant;

import com.rocketseat.planner.trip.Trip;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ParticipantService {
    @Autowired
    private ParticipantRepository repository;
    public void registerParticipantsToEvent(List<String> participantsToInvite, Trip trip){
       List<Participant> participants = participantsToInvite.stream().map(email->new Participant(email, trip)).toList();
       this.repository.saveAll(participants);
       System.out.println(participants.get(0).getId());
    }
    public void triggerConfirmationEmailToParticipants(UUID idTrip){}

    public void triggerConfirmationEmailToParticipant(String email) {
    }
    public ParticipantCreateResponse registerParticipant(String email,Trip trip){
        Participant participant = new Participant(email, trip);
        this.repository.save(participant);
        return new ParticipantCreateResponse(participant.getId());
    }

    public List<ParticipantData> getAllParticipantsFromEvent(UUID id) {
        return this.repository.findByTripId(id)
                .stream()
                .map(participant -> new ParticipantData(
                        participant.getId(),
                        participant.getEmail(),
                        participant.getName(),
                        participant.getIsConfirmed())).toList();
    }
}
