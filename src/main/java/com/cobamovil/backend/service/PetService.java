package com.cobamovil.backend.service;

import com.cobamovil.backend.dto.PetDTO;
import com.cobamovil.backend.entity.Pet;
import com.cobamovil.backend.entity.User;
import com.cobamovil.backend.repository.PetRepository;
import com.cobamovil.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PetService {
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    public PetService(PetRepository petRepository, UserRepository userRepository) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PetDTO> myPets(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return petRepository.findByOwner(user).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public PetDTO create(String username, PetDTO dto) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Pet p = new Pet();
        p.setOwner(user);
        apply(dto, p);
        return toDTO(petRepository.save(p));
    }

    @Transactional
    public PetDTO update(String username, Long id, PetDTO dto) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Pet p = petRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Pet not found"));
        if (!p.getOwner().getId().equals(user.getId())) throw new IllegalArgumentException("Unauthorized");
        apply(dto, p);
        return toDTO(petRepository.save(p));
    }

    @Transactional
    public void delete(String username, Long id) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Pet p = petRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Pet not found"));
        if (!p.getOwner().getId().equals(user.getId())) throw new IllegalArgumentException("Unauthorized");
        petRepository.delete(p);
    }

    private void apply(PetDTO dto, Pet p) {
        p.setName(dto.getName());
        p.setBreed(dto.getBreed());
        p.setSex(dto.getSex());
        p.setAge(dto.getAge());
        p.setWeight(dto.getWeight());
        p.setBehavior(dto.getBehavior());
        p.setHealthNotes(dto.getHealthNotes());
    }

    private PetDTO toDTO(Pet p) {
        PetDTO d = new PetDTO();
        d.setId(p.getId());
        d.setName(p.getName());
        d.setBreed(p.getBreed());
        d.setSex(p.getSex());
        d.setAge(p.getAge());
        d.setWeight(p.getWeight());
        d.setBehavior(p.getBehavior());
        d.setHealthNotes(p.getHealthNotes());
        return d;
    }
}

