package com.cobamovil.backend.service;

import com.cobamovil.backend.dto.GroomerCreateDTO;
import com.cobamovil.backend.dto.GroomerProfileDTO;
import com.cobamovil.backend.entity.CutRecord;
import com.cobamovil.backend.entity.GroomerProfile;
import com.cobamovil.backend.entity.ServiceType;
import com.cobamovil.backend.entity.User;
import com.cobamovil.backend.repository.CutRecordRepository;
import com.cobamovil.backend.repository.GroomerProfileRepository;
import com.cobamovil.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroomerService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroomerProfileRepository profileRepository;
    private final CutRecordRepository cutRecordRepository;

    public GroomerService(UserRepository userRepository, PasswordEncoder passwordEncoder, GroomerProfileRepository profileRepository, CutRecordRepository cutRecordRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.profileRepository = profileRepository;
        this.cutRecordRepository = cutRecordRepository;
    }

    @Transactional
    public GroomerProfileDTO createGroomer(GroomerCreateDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) throw new IllegalArgumentException("Username exists");
        if (userRepository.existsByEmail(dto.getEmail())) throw new IllegalArgumentException("Email exists");
        User u = new User(dto.getUsername(), dto.getEmail(), passwordEncoder.encode(dto.getPassword()), "GROOMER");
        if (dto.getPhone() != null) u.setPhone(dto.getPhone());
        User saved = userRepository.save(u);
        GroomerProfile p = new GroomerProfile();
        p.setUser(saved); p.setAvatarUrl(dto.getAvatarUrl()); p.setBio(dto.getBio()); p.setSpecialties(dto.getSpecialties());
        profileRepository.save(p);
        return toDTO(p);
    }

    @Transactional(readOnly = true)
    public List<GroomerProfileDTO> listGroomers() {
        var groomers = userRepository.findByRole("GROOMER");
        return groomers.stream()
                .map(u -> profileRepository.findByUser(u).orElseGet(() -> {
                    GroomerProfile gp = new GroomerProfile(); gp.setUser(u); return gp; }))
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GroomerProfileDTO getProfile(Long userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        GroomerProfile p = profileRepository.findByUser(u).orElseGet(() -> { GroomerProfile gp = new GroomerProfile(); gp.setUser(u); return gp; });
        return toDTO(p);
    }

    @Transactional(readOnly = true)
    public List<CutRecord> getHistory(Long userId) {
        User u = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return cutRecordRepository.findByGroomerOrderByDateDescTimeDesc(u);
    }

    @Transactional
    public GroomerProfileDTO updateProfile(Long userId, GroomerProfileDTO dto) {
        User u = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found"));
        GroomerProfile p = profileRepository.findByUser(u).orElseGet(() -> { GroomerProfile gp = new GroomerProfile(); gp.setUser(u); return gp; });
        p.setAvatarUrl(dto.getAvatarUrl()); p.setBio(dto.getBio()); p.setSpecialties(dto.getSpecialties());
        profileRepository.save(p);
        return toDTO(p);
    }

    private GroomerProfileDTO toDTO(GroomerProfile p) {
        GroomerProfileDTO d = new GroomerProfileDTO();
        d.setId(p.getId());
        d.setUserId(p.getUser() != null ? p.getUser().getId() : null);
        d.setUsername(p.getUser() != null ? p.getUser().getUsername() : null);
        d.setAvatarUrl(p.getAvatarUrl());
        d.setBio(p.getBio());
        d.setSpecialties(p.getSpecialties());
        return d;
    }
}

