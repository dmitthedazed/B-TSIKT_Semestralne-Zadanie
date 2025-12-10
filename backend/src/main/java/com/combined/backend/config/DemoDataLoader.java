package com.combined.backend.config;

import com.combined.backend.model.*;
import com.combined.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Profile("demo")
public class DemoDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final TaskRepository taskRepository;
    private final ResourceRepository resourceRepository;
    private final MembershipRepository membershipRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataLoader(UserRepository userRepository,
                          GroupRepository groupRepository,
                          TaskRepository taskRepository,
                          ResourceRepository resourceRepository,
                          MembershipRepository membershipRepository,
                          ActivityLogRepository activityLogRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.taskRepository = taskRepository;
        this.resourceRepository = resourceRepository;
        this.membershipRepository = membershipRepository;
        this.activityLogRepository = activityLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return; // already seeded

        // Teachers
        Map<String, User> teachers = new HashMap<>();
        teachers.put("prof. Ing. Peter Farkaš, DrSc.", createUser("Peter", "Farkaš", "pfarkas", "pfarkas@example.com", "password"));
        teachers.put("doc. Ing. Radoslav Vargic, PhD.", createUser("Radoslav", "Vargic", "rvargic", "rvargic@example.com", "password"));
        teachers.put("prof. Ing. Gregor Rozinaj, PhD.", createUser("Gregor", "Rozinaj", "grozinaj", "grozinaj@example.com", "password"));
        teachers.put("doc. Ing. Martin Rakús, PhD.", createUser("Martin", "Rakús", "mrakus", "mrakus@example.com", "password"));
        teachers.put("doc. Ing. Tomáš Páleník, PhD.", createUser("Tomáš", "Páleník", "tpalenik", "tpalenik@example.com", "password"));
        teachers.put("doc. Ing. Juraj Kačur, PhD.", createUser("Juraj", "Kačur", "jkacur", "jkacur@example.com", "password"));
        teachers.put("doc. Ing. Martin Medvecký, PhD.", createUser("Martin", "Medvecký", "mmedvecky", "mmedvecky@example.com", "password"));
        teachers.put("doc. Ing. Rastislav Róka, PhD.", createUser("Rastislav", "Róka", "rroka", "rroka@example.com", "password"));
        teachers.put("doc. Ing. Miloš Orgoň, PhD.", createUser("Miloš", "Orgoň", "morgon", "morgon@example.com", "password"));
        teachers.put("prof. Ing. Ivan Baroňák, PhD.", createUser("Ivan", "Baroňák", "ibaronak", "ibaronak@example.com", "password"));
        teachers.put("prof. Ing. Jaroslav Polec, PhD.", createUser("Jaroslav", "Polec", "jpolec", "jpolec@example.com", "password"));

        // Demo user for UI
        User demo = createUser("Demo", "User", "demo", "demo@example.com", "demo123");

        // Students
        List<User> students = createStudents();

        // Courses mapped to supervisors
        record Course(String code, String title, String period, String supervisor) {}
        List<Course> courses = List.of(
                new Course("D-ACD", "Advanced codes for transmission", "2025/2026 - post-graduate studies - FEI", "prof. Ing. Peter Farkaš, DrSc."),
                new Course("I-PIKT", "Advanced information and communication techniques", "WS 2025/2026 - FEI", "prof. Ing. Peter Farkaš, DrSc."),
                new Course("I-PMSOV", "Advanced Methods of Image and Video Processing", "WS 2025/2026 - FEI", "doc. Ing. Radoslav Vargic, PhD."),
                new Course("B-ACSS1", "Analog and Digital Signal Processing 1", "WS 2025/2026 - FEI", "prof. Ing. Gregor Rozinaj, PhD."),
                new Course("I-ABS5G", "Architectures of Wireless 4G and 5G+ Networks", "WS 2025/2026 - FEI", "doc. Ing. Martin Rakús, PhD."),
                new Course("B-KIS", "Communication and Information Networks", "WS 2025/2026 - FEI", "doc. Ing. Tomáš Páleník, PhD."),
                new Course("B-PS", "Computer networks", "WS 2025/2026 - FEI", "doc. Ing. Radoslav Vargic, PhD."),
                new Course("I-CSO", "Digital Image Processing", "WS 2025/2026 - FEI", "doc. Ing. Radoslav Vargic, PhD."),
                new Course("I-CSST", "Digital Signal Processing", "WS 2025/2026 - FEI", "prof. Ing. Gregor Rozinaj, PhD."),
                new Course("I-CSR", "Digital Speech Processing", "WS 2025/2026 - FEI", "doc. Ing. Juraj Kačur, PhD."),
                new Course("I-SIBU", "Future Networks", "WS 2025/2026 - FEI", "doc. Ing. Martin Medvecký, PhD."),
                new Course("B-ROCS", "Human-Machine Interface", "WS 2025/2026 - FEI", "doc. Ing. Juraj Kačur, PhD."),
                new Course("B-SPMM", "Multimedia Processing", "WS 2025/2026 - FEI", "prof. Ing. Gregor Rozinaj, PhD."),
                new Course("B-NGN", "Next Generation Networks", "WS 2025/2026 - FEI", "doc. Ing. Martin Medvecký, PhD."),
                new Course("B-OSD", "Operating Systems and Databases", "WS 2025/2026 - FEI", "doc. Ing. Martin Medvecký, PhD."),
                new Course("I-OKSS", "Optocommunications Systems and Networks", "WS 2025/2026 - FEI", "doc. Ing. Rastislav Róka, PhD."),
                new Course("I-BTSSS", "Security of Communication Systems, Networks and Services", "WS 2025/2026 - FEI", "doc. Ing. Miloš Orgoň, PhD."),
                new Course("B-TSIKT", "Software Development in ICT", "WS 2025/2026 - FEI", "doc. Ing. Radoslav Vargic, PhD."),
                new Course("I-SC", "Space Communication", "WS 2025/2026 - FEI", "prof. Ing. Peter Farkaš, DrSc."),
                new Course("I-RTS", "Telecommunication Networks Management", "WS 2025/2026 - FEI", "doc. Ing. Martin Medvecký, PhD.")
        );

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (Course c : courses) {
            User creator = teachers.getOrDefault(c.supervisor, demo);
            Group g = createGroup(c.code, c.title + " (" + c.period + ")", creator, LocalDateTime.now().minusDays(rnd.nextInt(3, 15)));
            addMember(creator, g, Membership.Role.CREATOR);
            addMember(demo, g, Membership.Role.ADMIN); // demo user participates everywhere
            // add 8-15 random students
            Collections.shuffle(students, rnd);
            int count = rnd.nextInt(8, 16);
            for (int i = 0; i < count; i++) {
                addMember(students.get(i), g, i == 0 ? Membership.Role.ADMIN : Membership.Role.MEMBER);
            }

            // tasks
            createTask(g, creator, students.get(0), "Kickoff", "Intro session and syllabus", "OPEN", LocalDateTime.now().plusDays(3));
            createTask(g, creator, students.get(1), "Assignment 1", "First homework", "IN_PROGRESS", LocalDateTime.now().plusDays(7));
            createTask(g, creator, students.get(2), "Quiz 1", "Short quiz on basics", "OPEN", LocalDateTime.now().plusDays(10));
            createTask(g, creator, students.get(3), "Project proposal", "Submit 1-page project idea", "OPEN", LocalDateTime.now().plusDays(14));
            createTask(g, creator, students.get(4), "Midterm review", "Review session prep", "OPEN", LocalDateTime.now().plusDays(18));
            createTask(g, creator, students.get(5), "Lab report", "Upload lab report as PDF", "IN_PROGRESS", LocalDateTime.now().plusDays(12));
            createTask(g, creator, students.get(6), "Peer feedback", "Provide feedback to peers", "OPEN", LocalDateTime.now().plusDays(9));
            createTask(g, creator, students.get(7), "Reading assignment", "Read Chapter 3-4", "OPEN", LocalDateTime.now().plusDays(5));

            // resources
            createResource(g, creator, "Syllabus", "https://example.com/" + c.code.toLowerCase() + "/syllabus.pdf", "LINK", "Course syllabus");
            createResource(g, creator, "Lecture slides", "https://example.com/" + c.code.toLowerCase() + "/slides-week1.pdf", "LINK", "Week 1 slides");
            createResource(g, creator, "Assignment template", "https://example.com/" + c.code.toLowerCase() + "/assignment-template.docx", "LINK", "Template for assignments");
            createResource(g, creator, "Reference book", "https://example.com/" + c.code.toLowerCase() + "/reference-book.pdf", "LINK", "Reference material");
            createResource(g, creator, "Lab materials", "https://example.com/" + c.code.toLowerCase() + "/lab.zip", "LINK", "Sample code and data for labs");
            createResource(g, creator, "FAQ", "https://example.com/" + c.code.toLowerCase() + "/faq", "LINK", "Common questions");

            log(g, creator, "COURSE_CREATED");
            addActivityHistory(g, Arrays.asList(creator, demo, students.get(0), students.get(1), students.get(2)));
        }
    }

    private User createUser(String first, String last, String nick, String email, String rawPassword) {
        User u = new User();
        u.setFirstName(first);
        u.setLastName(last);
        u.setNickname(nick);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        return userRepository.save(u);
    }

    private Group createGroup(String name, String description, User creator, LocalDateTime createdAt) {
        Group g = new Group();
        g.setName(name);
        g.setDescription(description);
        g.setCreatedBy(creator);
        g.setCreatedAt(createdAt);
        return groupRepository.save(g);
    }

    private void addMember(User user, Group group, Membership.Role role) {
        Membership m = new Membership();
        m.setUser(user);
        m.setGroup(group);
        m.setRole(role);
        m.setJoinedAt(LocalDateTime.now().minusDays(1));
        membershipRepository.save(m);
    }

    private void createTask(Group group, User creator, User assignee, String title, String description, String status, LocalDateTime deadline) {
        Task t = new Task();
        t.setGroup(group);
        t.setCreatedBy(creator);
        t.setAssignedTo(assignee);
        t.setTitle(title);
        t.setDescription(description);
        t.setStatus(status);
        t.setDeadline(deadline);
        t.setCreatedAt(LocalDateTime.now().minusDays(1));
        taskRepository.save(t);
    }

    private void createResource(Group group, User uploader, String title, String pathOrUrl, String type, String description) {
        Resource r = new Resource();
        r.setGroup(group);
        r.setUploadedBy(uploader);
        r.setTitle(title);
        r.setPathOrUrl(pathOrUrl);
        r.setType(type);
        r.setDescription(description);
        r.setUploadedAt(LocalDateTime.now().minusDays(1));
        resourceRepository.save(r);
    }

    private void log(Group group, User user, String action) {
        ActivityLog log = new ActivityLog();
        log.setGroup(group);
        log.setUser(user);
        log.setActivityType(action);
        log.setDescription(action + " event");
        log.setTimestamp(LocalDateTime.now().minusHours(2));
        activityLogRepository.save(log);
    }

    private void addActivityHistory(Group group, List<User> users) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        LocalDateTime now = LocalDateTime.now();
        String[] actions = {"TASK_CREATED", "TASK_UPDATED", "TASK_DONE", "RESOURCE_ADDED", "MEMBER_ADDED"};

        for (int i = 0; i < 180; i++) {
            User u = users.get(rnd.nextInt(users.size()));
            String action = actions[rnd.nextInt(actions.length)];
            ActivityLog log = new ActivityLog();
            log.setGroup(group);
            log.setUser(u);
            log.setActivityType(action);
            log.setDescription(action + " event");
            long daysBack = rnd.nextInt(0, 90);
            long hoursBack = rnd.nextInt(0, 24);
            log.setTimestamp(now.minusDays(daysBack).minusHours(hoursBack));
            activityLogRepository.save(log);
        }
    }

    private List<User> createStudents() {
        String[][] names = {
                {"Alex", "Novak"}, {"Bella", "Klein"}, {"Chris", "Morgan"}, {"Diana", "Lopez"},
                {"Ethan", "Patel"}, {"Fiona", "Wang"}, {"George", "Brown"}, {"Hana", "Kovac"},
                {"Ivan", "Horvath"}, {"Julia", "Markova"}, {"Kevin", "Smith"}, {"Laura", "Garcia"},
                {"Marek", "Svic"}, {"Nina", "Kral"}, {"Oscar", "Diaz"}, {"Petra", "Zemanova"},
                {"Quinn", "Lee"}, {"Rasto", "Urban"}, {"Sara", "Mihalik"}, {"Tomas", "Kovacs"},
                {"Una", "Vesela"}, {"Viktor", "Krupa"}
        };
        List<User> list = new ArrayList<>();
        for (String[] n : names) {
            String first = n[0];
            String last = n[1];
            String nick = (first.charAt(0) + last).toLowerCase();
            String email = nick + "@student.demo";
            list.add(createUser(first, last, nick, email, "student123"));
        }
        return list;
    }
}
