package com.mallowlink.auth.repository;

import com.mallowlink.auth.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    
    Optional<Client> findByClientId(String clientId);
    
    Optional<Client> findByClientKey(String uuid);
    
    boolean existsByClientId(String clientId);
    
    boolean existsByClientKey(String uuid);
    
    void deleteByClientId(String clientId);
}