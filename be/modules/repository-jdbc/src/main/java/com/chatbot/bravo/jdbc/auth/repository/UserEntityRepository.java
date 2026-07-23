package com.chatbot.bravo.jdbc.auth.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface UserEntityRepository extends CrudRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameAndIsDeletedFalse(String username);
}
