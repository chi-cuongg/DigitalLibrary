package com.example.demo.repository;

import com.example.demo.model.OAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {
    // Since we only store one token at a time, get the latest one
    Optional<OAuthToken> findFirstByOrderByUpdatedAtDesc();
}
