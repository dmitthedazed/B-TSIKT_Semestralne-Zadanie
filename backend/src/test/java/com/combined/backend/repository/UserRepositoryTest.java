package com.combined.backend.repository;

import com.combined.backend.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User buildUser(String email, String nickname) {
        User u = new User();
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail(email);
        u.setNickname(nickname);
        u.setPasswordHash("hashedpw"); // meets @Size(min = 6)
        return u;
    }

    @Test
    void findsByEmailAndNickname() {
        User u = userRepository.save(buildUser("a@example.com", "nick1"));

        assertThat(userRepository.findByEmail("a@example.com")).isPresent();
        assertThat(userRepository.findByNickname("nick1")).contains(u);
    }

    @Test
    void enforcesUniqueEmailAndNickname() {
        userRepository.save(buildUser("dup@example.com", "nickA"));
        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(buildUser("dup@example.com", "nickB"));
        }).isInstanceOf(DataIntegrityViolationException.class);

        userRepository.save(buildUser("uniq@example.com", "nickC"));
        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(buildUser("other@example.com", "nickC"));
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
