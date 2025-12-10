package com.combined.backend.repository;

import com.combined.backend.model.Group;
import com.combined.backend.model.Membership;
import com.combined.backend.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class MembershipRepositoryTest {

    @Autowired private MembershipRepository membershipRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;

    private User user(String email, String nick) {
        User u = new User();
        u.setFirstName("A");
        u.setEmail(email);
        u.setNickname(nick);
        u.setPasswordHash("hashedpw"); // meets @Size(min = 6)
        return userRepository.save(u);
    }

    private Group group(User creator) {
        Group g = new Group();
        g.setName("Group-1");
        g.setDescription("Group description");
        g.setCreatedBy(creator);
        g.setCreatedAt(LocalDateTime.now());
        return groupRepository.save(g);
    }

    @Test
    void existsAndCountWork() {
        User u1 = user("a@example.com", "a");
        User u2 = user("b@example.com", "b");
        Group g = group(u1);

        Membership m1 = new Membership(u1, g, Membership.Role.CREATOR, LocalDateTime.now());
        Membership m2 = new Membership(u2, g, Membership.Role.MEMBER, LocalDateTime.now());
        membershipRepository.save(m1);
        membershipRepository.save(m2);

        assertThat(membershipRepository.existsByUserAndGroup(u1, g)).isTrue();
        assertThat(membershipRepository.existsByUserAndGroup(u2, g)).isTrue();
        assertThat(membershipRepository.countByGroup(g)).isEqualTo(2);
    }
}
