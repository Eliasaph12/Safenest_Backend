package com.safenest.service;

import com.safenest.model.ActivityLogRecord;
import com.safenest.model.AppointmentRecord;
import com.safenest.model.CaseNoteRecord;
import com.safenest.model.ChatMessageRecord;
import com.safenest.model.ChatSessionRecord;
import com.safenest.model.LegalCaseRecord;
import com.safenest.model.OtpVerificationRecord;
import com.safenest.model.ResourceRecord;
import com.safenest.model.UserAccount;
import com.safenest.repository.ActivityLogRepository;
import com.safenest.repository.AppointmentRepository;
import com.safenest.repository.CaseNoteRepository;
import com.safenest.repository.ChatMessageRepository;
import com.safenest.repository.ChatSessionRepository;
import com.safenest.repository.LegalCaseRepository;
import com.safenest.repository.OtpVerificationRepository;
import com.safenest.repository.ResourceRepository;
import com.safenest.repository.UserAccountRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class PlatformService {

    private static final String OTP_PURPOSE_LOGIN = "LOGIN";
    private static final String OTP_PURPOSE_REGISTER = "REGISTER";

    private final UserAccountRepository userAccountRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ResourceRepository resourceRepository;
    private final AppointmentRepository appointmentRepository;
    private final CaseNoteRepository caseNoteRepository;
    private final LegalCaseRepository legalCaseRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AgentNotificationService agentNotificationService;
    private final int otpExpiryMinutes;
    private final boolean otpDevMode;

    public PlatformService(
        UserAccountRepository userAccountRepository,
        ActivityLogRepository activityLogRepository,
        ResourceRepository resourceRepository,
        AppointmentRepository appointmentRepository,
        CaseNoteRepository caseNoteRepository,
        LegalCaseRepository legalCaseRepository,
        ChatMessageRepository chatMessageRepository,
        ChatSessionRepository chatSessionRepository,
        OtpVerificationRepository otpVerificationRepository,
        PasswordEncoder passwordEncoder,
        AgentNotificationService agentNotificationService,
        @Value("${app.otp.expiry-minutes:10}") int otpExpiryMinutes,
        @Value("${app.otp.dev-mode:true}") boolean otpDevMode
    ) {
        this.userAccountRepository = userAccountRepository;
        this.activityLogRepository = activityLogRepository;
        this.resourceRepository = resourceRepository;
        this.appointmentRepository = appointmentRepository;
        this.caseNoteRepository = caseNoteRepository;
        this.legalCaseRepository = legalCaseRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.otpVerificationRepository = otpVerificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.agentNotificationService = agentNotificationService;
        this.otpExpiryMinutes = otpExpiryMinutes;
        this.otpDevMode = otpDevMode;
    }

    @PostConstruct
    void init() {
        seedResourcesIfEmpty();
        seedOperationalDataIfEmpty();
    }

    public Map<String, Object> login(String email, String password) {
        return requestLoginOtp(email, password, null);
    }

    public Map<String, Object> register(Map<String, Object> request) {
        return requestRegistrationOtp(request);
    }

    public Map<String, Object> requestRegistrationOtp(Map<String, Object> request) {
        String name = stringValue(request.get("name"), "").trim();
        String email = stringValue(request.get("email"), "").trim().toLowerCase();
        String password = stringValue(request.get("password"), "");
        String role = stringValue(request.get("role"), "Victim");
        String phoneNumber = normalizePhoneNumber(request.get("phoneNumber"));

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            return mapOf("error", "Name, email, password, and phone number are required");
        }
        if (phoneNumber == null) {
            return mapOf("error", "Enter a valid phone number");
        }
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            return mapOf("error", "Email already registered");
        }

        UserAccount phoneOwner = findByPhoneNumber(phoneNumber);
        if (phoneOwner != null) {
            return mapOf("error", "Phone number already registered");
        }

        consumePendingOtpsForEmail(email, OTP_PURPOSE_REGISTER);

        OtpVerificationRecord otp = new OtpVerificationRecord();
        otp.setPurpose(OTP_PURPOSE_REGISTER);
        otp.setEmailAddress(email);
        otp.setPhoneNumber(phoneNumber);
        otp.setFullName(name);
        otp.setPendingRole(role);
        otp.setPasswordHash(passwordEncoder.encode(password));
        otp.setCode(generateOtpCode());
        otp.setExpiresAt(Instant.now().plusSeconds(otpExpiryMinutes * 60L));
        OtpVerificationRecord saved = otpVerificationRepository.save(otp);

        return otpChallengeResponse(saved, "Registration OTP generated successfully.");
    }

    @Transactional
    public Map<String, Object> verifyRegistrationOtp(Long verificationId, String otpCode) {
        OtpVerificationRecord otp = validateOtp(verificationId, otpCode, OTP_PURPOSE_REGISTER);
        if (otp == null) {
            return mapOf("error", "Invalid or expired OTP");
        }

        if (userAccountRepository.existsByEmailIgnoreCase(stringValue(otp.getEmailAddress(), ""))) {
            otp.setConsumed(true);
            otpVerificationRepository.save(otp);
            return mapOf("error", "Email already registered");
        }

        UserAccount phoneOwner = findByPhoneNumber(otp.getPhoneNumber());
        if (phoneOwner != null) {
            otp.setConsumed(true);
            otpVerificationRepository.save(otp);
            return mapOf("error", "Phone number already registered");
        }

        UserAccount newUser = new UserAccount();
        newUser.setName(stringValue(otp.getFullName(), "New User"));
        newUser.setEmail(stringValue(otp.getEmailAddress(), "").trim().toLowerCase());
        newUser.setPhoneNumber(otp.getPhoneNumber());
        newUser.setPhoneVerified(true);
        newUser.setRole(stringValue(otp.getPendingRole(), "Victim"));
        newUser.setPassword(otp.getPasswordHash());

        UserAccount savedUser = userAccountRepository.save(newUser);
        otp.setConsumed(true);
        otp.setUserId(savedUser.getId());
        otpVerificationRepository.save(otp);
        logUserActivity(savedUser, "USER_REGISTERED", "USER", savedUser.getId(), savedUser.getName() + " created a verified account.", savedUser.getPhoneNumber());
        return authSuccessResponse(savedUser);
    }

    public Map<String, Object> requestLoginOtp(String email, String password, String requestedPhoneNumber) {
        String normalizedEmail = stringValue(email, "").trim().toLowerCase();
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return mapOf("error", "Invalid credentials");
        }

        String storedPhoneNumber = normalizePhoneNumber(user.getPhoneNumber());
        String fallbackPhoneNumber = normalizePhoneNumber(requestedPhoneNumber);

        if (storedPhoneNumber == null && fallbackPhoneNumber == null) {
            return mapOf("error", "This account does not have a phone number yet. Enter your phone number to continue.");
        }

        if (storedPhoneNumber != null && fallbackPhoneNumber != null && !storedPhoneNumber.equals(fallbackPhoneNumber)) {
            return mapOf("error", "Phone number does not match the number registered on this account");
        }

        String otpPhoneNumber = storedPhoneNumber != null ? storedPhoneNumber : fallbackPhoneNumber;
        UserAccount phoneOwner = findByPhoneNumber(otpPhoneNumber);
        if (storedPhoneNumber == null && phoneOwner != null && !Objects.equals(phoneOwner.getId(), user.getId())) {
            return mapOf("error", "Phone number already registered to another account");
        }

        consumePendingOtpsForEmail(normalizedEmail, OTP_PURPOSE_LOGIN);

        OtpVerificationRecord otp = new OtpVerificationRecord();
        otp.setPurpose(OTP_PURPOSE_LOGIN);
        otp.setEmailAddress(normalizedEmail);
        otp.setPhoneNumber(otpPhoneNumber);
        otp.setUserId(user.getId());
        otp.setCode(generateOtpCode());
        otp.setExpiresAt(Instant.now().plusSeconds(otpExpiryMinutes * 60L));
        OtpVerificationRecord saved = otpVerificationRepository.save(otp);

        return otpChallengeResponse(saved, "Login OTP generated successfully.");
    }

    @Transactional
    public Map<String, Object> verifyLoginOtp(Long verificationId, String otpCode) {
        OtpVerificationRecord otp = validateOtp(verificationId, otpCode, OTP_PURPOSE_LOGIN);
        if (otp == null) {
            return mapOf("error", "Invalid or expired OTP");
        }

        UserAccount user = otp.getUserId() != null ? userAccountRepository.findById(otp.getUserId()).orElse(null) : null;
        if (user == null) {
            otp.setConsumed(true);
            otpVerificationRepository.save(otp);
            return mapOf("error", "User account not found");
        }

        if (normalizePhoneNumber(user.getPhoneNumber()) == null) {
            UserAccount phoneOwner = findByPhoneNumber(otp.getPhoneNumber());
            if (phoneOwner != null && !Objects.equals(phoneOwner.getId(), user.getId())) {
                otp.setConsumed(true);
                otpVerificationRepository.save(otp);
                return mapOf("error", "Phone number already registered to another account");
            }
            user.setPhoneNumber(otp.getPhoneNumber());
        }

        user.setPhoneVerified(true);
        UserAccount savedUser = userAccountRepository.save(user);
        otp.setConsumed(true);
        otpVerificationRepository.save(otp);
        logUserActivity(savedUser, "USER_LOGIN", "USER", savedUser.getId(), savedUser.getName() + " completed OTP login.", savedUser.getPhoneNumber());
        return authSuccessResponse(savedUser);
    }

    public Map<String, Object> health() {
        return mapOf("status", "OK", "timestamp", Instant.now().toString());
    }

    public List<Map<String, Object>> getAllResources() {
        return resourceRepository.findAllByOrderByPriorityLevelDescUpdatedAtDesc().stream()
            .map(this::resourceView)
            .collect(Collectors.toList());
    }

    public Map<String, Object> getResourceById(Long id) {
        return resourceRepository.findById(id).map(this::resourceView).orElse(null);
    }

    public Map<String, Object> createResource(Map<String, Object> request) {
        ResourceRecord resource = new ResourceRecord();
        applyResourceRequest(resource, request);
        ResourceRecord saved = resourceRepository.save(resource);
        logActivity(null, "SYSTEM", "RESOURCE_CREATED", "RESOURCE", saved.getId(), "Resource created: " + saved.getName(), saved.getResourceType());
        return resourceView(saved);
    }

    public Map<String, Object> updateResource(Long id, Map<String, Object> request) {
        ResourceRecord resource = resourceRepository.findById(id).orElse(null);
        if (resource == null) {
            return null;
        }
        applyResourceRequest(resource, request);
        ResourceRecord saved = resourceRepository.save(resource);
        logActivity(null, "SYSTEM", "RESOURCE_UPDATED", "RESOURCE", saved.getId(), "Resource updated: " + saved.getName(), saved.getResourceType());
        return resourceView(saved);
    }

    public boolean deleteResource(Long id) {
        ResourceRecord resource = resourceRepository.findById(id).orElse(null);
        if (resource == null) {
            return false;
        }
        resourceRepository.deleteById(id);
        logActivity(null, "SYSTEM", "RESOURCE_DELETED", "RESOURCE", id, "Resource deleted.", resource.getName());
        return true;
    }

    public List<Map<String, Object>> getCounsellorAppointments(Integer counsellorId) {
        return appointmentRepository.findByCounsellorIdOrderByAppointmentDateTimeDesc(counsellorId.longValue()).stream()
            .map(this::appointmentView)
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getVictimAppointments(Integer victimId) {
        return appointmentRepository.findByVictimIdOrderByAppointmentDateTimeDesc(victimId.longValue()).stream()
            .map(this::appointmentView)
            .collect(Collectors.toList());
    }

    public Map<String, Object> createAppointment(Map<String, Object> request) {
        AppointmentRecord appointment = new AppointmentRecord();
        appointment.setVictimId(requiredLong(request.get("victimId")));
        appointment.setCounsellorId(requiredLong(request.get("counsellorId")));
        appointment.setType(stringValue(request.get("type"), "VIDEO_CALL"));
        appointment.setAppointmentDateTime(parseLocalDateTime(request.get("appointmentDateTime")));
        appointment.setStatus(stringValue(request.get("status"), "SCHEDULED"));
        appointment.setNotes(stringValue(request.get("notes"), ""));
        AppointmentRecord saved = appointmentRepository.save(appointment);
        logActivity(saved.getCounsellorId(), "Counsellor", "APPOINTMENT_CREATED", "APPOINTMENT", saved.getId(), "Appointment scheduled.", "Victim " + saved.getVictimId());
        return appointmentView(saved);
    }

    public boolean deleteAppointment(Integer id) {
        AppointmentRecord appointment = appointmentRepository.findById(id.longValue()).orElse(null);
        if (appointment == null) {
            return false;
        }
        appointmentRepository.deleteById(id.longValue());
        logActivity(appointment.getCounsellorId(), "Counsellor", "APPOINTMENT_DELETED", "APPOINTMENT", appointment.getId(), "Appointment deleted.", appointment.getType());
        return true;
    }

    public List<Map<String, Object>> getCounsellorCaseNotes(Integer counsellorId) {
        return caseNoteRepository.findByCounsellorIdOrderByCreatedAtDesc(counsellorId.longValue()).stream()
            .map(this::caseNoteView)
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getVictimCaseNotes(Integer victimId) {
        return caseNoteRepository.findByVictimIdOrderByCreatedAtDesc(victimId.longValue()).stream()
            .map(this::caseNoteView)
            .collect(Collectors.toList());
    }

    public Map<String, Object> createCaseNote(Map<String, Object> request) {
        CaseNoteRecord note = new CaseNoteRecord();
        note.setVictimId(requiredLong(request.get("victimId")));
        note.setCounsellorId(requiredLong(request.get("counsellorId")));
        note.setNoteContent(stringValue(request.get("noteContent"), ""));
        note.setCategory(stringValue(request.get("category"), "SESSION_NOTES"));
        note.setConfidential(parseBoolean(request.get("isConfidential"), false));
        CaseNoteRecord saved = caseNoteRepository.save(note);
        logActivity(saved.getCounsellorId(), "Counsellor", "CASE_NOTE_CREATED", "CASE_NOTE", saved.getId(), "Case note created.", saved.getCategory());
        return caseNoteView(saved);
    }

    public boolean deleteCaseNote(Integer id) {
        CaseNoteRecord note = caseNoteRepository.findById(id.longValue()).orElse(null);
        if (note == null) {
            return false;
        }
        caseNoteRepository.deleteById(id.longValue());
        logActivity(note.getCounsellorId(), "Counsellor", "CASE_NOTE_DELETED", "CASE_NOTE", note.getId(), "Case note deleted.", note.getCategory());
        return true;
    }

    public List<Map<String, Object>> getAdvisorCases(Integer advisorId) {
        return legalCaseRepository.findByAdvisorIdOrderByCreatedAtDesc(advisorId.longValue()).stream()
            .map(this::legalCaseView)
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getVictimCases(Integer victimId) {
        return legalCaseRepository.findByVictimIdOrderByCreatedAtDesc(victimId.longValue()).stream()
            .map(this::legalCaseView)
            .collect(Collectors.toList());
    }

    public Map<String, Object> createLegalCase(Map<String, Object> request) {
        LegalCaseRecord legalCase = new LegalCaseRecord();
        legalCase.setVictimId(requiredLong(request.get("victimId")));
        legalCase.setAdvisorId(requiredLong(request.get("advisorId")));
        legalCase.setTitle(stringValue(request.get("title"), stringValue(request.get("caseTitle"), "Untitled Case")));
        legalCase.setSummary(stringValue(request.get("summary"), stringValue(request.get("description"), "")));
        legalCase.setCaseType(stringValue(request.get("caseType"), "OTHER"));
        legalCase.setCaseNumber(buildCaseNumber());
        legalCase.setStatus(stringValue(request.get("status"), "OPEN"));
        LegalCaseRecord saved = legalCaseRepository.save(legalCase);
        logActivity(saved.getAdvisorId(), "LegalAdvisor", "LEGAL_CASE_CREATED", "LEGAL_CASE", saved.getId(), "Legal case created: " + saved.getTitle(), saved.getCaseType());
        return legalCaseView(saved);
    }

    public boolean deleteLegalCase(Integer id) {
        LegalCaseRecord legalCase = legalCaseRepository.findById(id.longValue()).orElse(null);
        if (legalCase == null) {
            return false;
        }
        legalCaseRepository.deleteById(id.longValue());
        logActivity(legalCase.getAdvisorId(), "LegalAdvisor", "LEGAL_CASE_DELETED", "LEGAL_CASE", legalCase.getId(), "Legal case deleted.", legalCase.getTitle());
        return true;
    }

    public List<Map<String, Object>> getChatMessages(Integer senderId, Integer receiverId, Long sessionId) {
        if (sessionId != null) {
            return chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId).stream()
                .map(this::chatMessageView)
                .collect(Collectors.toList());
        }
        return chatMessageRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverIdOrderByTimestampAsc(
                senderId.longValue(),
                receiverId.longValue(),
                receiverId.longValue(),
                senderId.longValue()
            ).stream()
            .map(this::chatMessageView)
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getAgentChatThreads(Integer agentId) {
        return chatSessionRepository.findByAgentIdOrderByUpdatedAtDesc(agentId.longValue()).stream()
            .map(this::chatSessionThreadView)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getVictimChatSessions(Integer victimId) {
        return chatSessionRepository.findByVictimIdOrderByUpdatedAtDesc(victimId.longValue()).stream()
            .map(this::chatSessionVictimView)
            .collect(Collectors.toList());
    }

    public Map<String, Object> createChatSession(Integer victimId, Integer agentId) {
        ChatSessionRecord existingActiveSession = chatSessionRepository.findByVictimIdOrderByUpdatedAtDesc(victimId.longValue()).stream()
            .filter(session -> agentId.longValue() == session.getAgentId() && "ACTIVE".equalsIgnoreCase(stringValue(session.getStatus(), "")))
            .findFirst()
            .orElse(null);

        if (existingActiveSession != null) {
            Map<String, Object> existingSessionView = new LinkedHashMap<>(chatSessionVictimView(existingActiveSession));
            existingSessionView.put("reusedExisting", true);
            return existingSessionView;
        }

        ChatSessionRecord session = new ChatSessionRecord();
        session.setVictimId(victimId.longValue());
        session.setAgentId(agentId.longValue());
        session.setStatus("ACTIVE");
        ChatSessionRecord saved = chatSessionRepository.save(session);
        Map<String, Object> createdSessionView = new LinkedHashMap<>(chatSessionVictimView(saved));
        createdSessionView.put("reusedExisting", false);
        logActivity(saved.getVictimId(), "Victim", "CHAT_SESSION_CREATED", "CHAT_SESSION", saved.getId(), "Support chat session started.", "Agent " + saved.getAgentId());
        return createdSessionView;
    }

    public Map<String, Object> closeChatSession(Long sessionId) {
        ChatSessionRecord session = chatSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return null;
        }
        session.setStatus("CLOSED");
        ChatSessionRecord saved = chatSessionRepository.save(session);
        logActivity(saved.getVictimId(), "Victim", "CHAT_SESSION_CLOSED", "CHAT_SESSION", saved.getId(), "Support chat session closed.", "Agent " + saved.getAgentId());
        return chatSessionVictimView(saved);
    }

    @Transactional
    public Map<String, Object> deleteChatSession(Long sessionId) {
        ChatSessionRecord session = chatSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return null;
        }

        Map<String, Object> deletedSession = new LinkedHashMap<>(chatSessionVictimView(session));
        chatMessageRepository.deleteBySessionId(sessionId);
        chatSessionRepository.deleteById(sessionId);
        deletedSession.put("deleted", true);
        logActivity(session.getVictimId(), "Victim", "CHAT_SESSION_DELETED", "CHAT_SESSION", sessionId, "Support chat session deleted.", "Agent " + session.getAgentId());
        return deletedSession;
    }

    public List<Map<String, Object>> getAuthorizedSupportAgents() {
        return userAccountRepository.findByRoleInOrderByNameAsc(Arrays.asList("Counsellor", "LegalAdvisor", "Admin")).stream()
            .filter(user -> user != null && isAuthorizedSupportRole(user.getRole()))
            .map(user -> mapOf(
                "id", user.getId(),
                "name", stringValue(user.getName(), "Support Agent"),
                "email", stringValue(user.getEmail(), ""),
                "role", normalizedRole(user.getRole()),
                "status", "ONLINE",
                "specialty", agentSpecialty(user.getRole())
            ))
            .collect(Collectors.toList());
    }

    public Map<String, Object> getPrimarySupportAgent() {
        List<UserAccount> agents = userAccountRepository.findByRoleInOrderByNameAsc(Arrays.asList("Counsellor", "LegalAdvisor", "Admin")).stream()
            .filter(user -> user != null && isAuthorizedSupportRole(user.getRole()))
            .collect(Collectors.toList());
        if (agents.isEmpty()) {
            return null;
        }

        UserAccount preferredAgent = agents.stream()
            .filter(user -> user.getEmail() != null && !user.getEmail().toLowerCase().endsWith("@example.com"))
            .findFirst()
            .orElse(agents.get(0));

        return mapOf(
            "id", preferredAgent.getId(),
            "name", stringValue(preferredAgent.getName(), "Support Agent"),
            "email", stringValue(preferredAgent.getEmail(), ""),
            "role", normalizedRole(preferredAgent.getRole()),
            "status", "ONLINE",
            "specialty", agentSpecialty(preferredAgent.getRole())
        );
    }

    public Map<String, Object> sendChatMessage(Map<String, Object> request) {
        Integer senderId = parseInteger(request.get("senderId"));
        Integer receiverId = parseInteger(request.get("receiverId"));
        Long sessionId = parseLong(request.get("sessionId"));
        String messageText = stringValue(request.get("message"), "").trim();

        if (senderId == null || receiverId == null || sessionId == null || messageText.isBlank()) {
            return mapOf("error", "senderId, receiverId, sessionId, and message are required");
        }

        ChatMessageRecord record = new ChatMessageRecord();
        record.setSenderId(senderId.longValue());
        record.setReceiverId(receiverId.longValue());
        record.setSessionId(sessionId);
        record.setMessage(messageText);

        ChatMessageRecord saved = chatMessageRepository.save(record);
        touchSession(sessionId);
        UserAccount sender = userAccountRepository.findById(saved.getSenderId()).orElse(null);
        logActivity(saved.getSenderId(), sender != null ? normalizedRole(sender.getRole()) : "User", "CHAT_MESSAGE_SENT", "CHAT_MESSAGE", saved.getId(), "Chat message sent.", abbreviate(saved.getMessage(), 120));
        Map<String, Object> response = chatMessageView(saved);
        notifyAgentIfNeeded(response);
        return response;
    }

    public List<Map<String, Object>> getUsers() {
        return userAccountRepository.findAll().stream().map(this::publicUser).collect(Collectors.toList());
    }

    public Map<String, Object> getUserById(Integer id) {
        return userAccountRepository.findById(id.longValue()).map(this::publicUser).orElse(null);
    }

    public boolean deleteUser(Integer id) {
        UserAccount user = userAccountRepository.findById(id.longValue()).orElse(null);
        if (user == null) {
            return false;
        }
        userAccountRepository.deleteById(id.longValue());
        logActivity(user.getId(), normalizedRole(user.getRole()), "USER_DELETED", "USER", user.getId(), "User account deleted.", user.getEmail());
        return true;
    }

    public Map<String, Integer> getSystemStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalUsers", Math.toIntExact(userAccountRepository.count()));
        stats.put("victimCount", userAccountRepository.countByRole("Victim"));
        stats.put("counsellorCount", userAccountRepository.countByRole("Counsellor"));
        stats.put("legalAdvisorCount", userAccountRepository.countByRole("LegalAdvisor"));
        stats.put("adminCount", userAccountRepository.countByRole("Admin"));
        stats.put("resourceCount", Math.toIntExact(resourceRepository.count()));
        return stats;
    }

    public List<Map<String, Object>> getRecentActivities() {
        return activityLogRepository.findTop20ByOrderByCreatedAtDesc().stream()
            .map(this::activityView)
            .collect(Collectors.toList());
    }

    private Map<String, Object> authSuccessResponse(UserAccount user) {
        return mapOf("token", "mock-jwt-token-" + user.getId(), "user", publicUser(user));
    }

    private Map<String, Object> otpChallengeResponse(OtpVerificationRecord otp, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requiresOtp", true);
        response.put("verificationId", otp.getId());
        response.put("message", message);
        response.put("phoneNumberHint", maskPhoneNumber(otp.getPhoneNumber()));
        response.put("expiresAt", otp.getExpiresAt().toString());
        if (otpDevMode) {
            response.put("otpPreview", otp.getCode());
        }
        return response;
    }

    private OtpVerificationRecord validateOtp(Long verificationId, String otpCode, String purpose) {
        if (verificationId == null || stringValue(otpCode, "").isBlank()) {
            return null;
        }
        OtpVerificationRecord otp = otpVerificationRepository.findByIdAndConsumedFalse(verificationId).orElse(null);
        if (otp == null) {
            return null;
        }
        if (!purpose.equalsIgnoreCase(stringValue(otp.getPurpose(), ""))) {
            return null;
        }
        if (otp.getExpiresAt() == null || otp.getExpiresAt().isBefore(Instant.now())) {
            return null;
        }
        if (!stringValue(otp.getCode(), "").equals(stringValue(otpCode, "").trim())) {
            return null;
        }
        return otp;
    }

    private void consumePendingOtpsForEmail(String email, String purpose) {
        otpVerificationRepository.findByEmailAddressAndPurposeAndConsumedFalse(email, purpose)
            .forEach(record -> {
                record.setConsumed(true);
                otpVerificationRepository.save(record);
            });
    }

    private UserAccount findByPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return userAccountRepository.findAll().stream()
            .filter(user -> phoneNumber.equals(normalizePhoneNumber(user.getPhoneNumber())))
            .findFirst()
            .orElse(null);
    }

    private String normalizePhoneNumber(Object rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            return null;
        }
        String digitsOnly = rawPhoneNumber.toString().replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 10 || digitsOnly.length() > 15) {
            return null;
        }
        return digitsOnly;
    }

    private String generateOtpCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String maskPhoneNumber(String phoneNumber) {
        String normalized = normalizePhoneNumber(phoneNumber);
        if (normalized == null || normalized.length() < 4) {
            return "hidden";
        }
        return "******" + normalized.substring(normalized.length() - 4);
    }

    private Map<String, Object> publicUser(UserAccount user) {
        return mapOf(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "phoneNumber", stringValue(user.getPhoneNumber(), ""),
            "phoneVerified", Boolean.TRUE.equals(user.getPhoneVerified()),
            "role", user.getRole()
        );
    }

    private Map<String, Object> activityView(ActivityLogRecord activity) {
        return mapOf(
            "id", activity.getId(),
            "actorUserId", activity.getActorUserId(),
            "actorName", activity.getActorName(),
            "actorRole", activity.getActorRole(),
            "actionType", activity.getActionType(),
            "subjectType", activity.getSubjectType(),
            "subjectId", activity.getSubjectId(),
            "description", activity.getDescription(),
            "details", stringValue(activity.getDetails(), ""),
            "createdAt", activity.getCreatedAt().toString()
        );
    }

    private Map<String, Object> resourceView(ResourceRecord resource) {
        return mapOf(
            "id", resource.getId(),
            "name", resource.getName(),
            "title", resource.getName(),
            "description", resource.getDescription(),
            "resourceType", resource.getResourceType(),
            "category", resource.getResourceType(),
            "targetAudience", resource.getTargetAudience(),
            "contactInfo", stringValue(resource.getContactInfo(), ""),
            "emergencyHotline", stringValue(resource.getEmergencyHotline(), ""),
            "priorityLevel", resource.getPriorityLevel(),
            "isActive", resource.getActive(),
            "createdAt", resource.getCreatedAt().toString(),
            "updatedAt", resource.getUpdatedAt().toString()
        );
    }

    private Map<String, Object> appointmentView(AppointmentRecord appointment) {
        return mapOf(
            "id", appointment.getId(),
            "victimId", appointment.getVictimId(),
            "counsellorId", appointment.getCounsellorId(),
            "type", appointment.getType(),
            "appointmentDateTime", appointment.getAppointmentDateTime().toString(),
            "status", appointment.getStatus(),
            "notes", stringValue(appointment.getNotes(), ""),
            "createdAt", appointment.getCreatedAt().toString(),
            "updatedAt", appointment.getUpdatedAt().toString()
        );
    }

    private Map<String, Object> caseNoteView(CaseNoteRecord note) {
        return mapOf(
            "id", note.getId(),
            "victimId", note.getVictimId(),
            "counsellorId", note.getCounsellorId(),
            "noteContent", note.getNoteContent(),
            "category", note.getCategory(),
            "createdAt", note.getCreatedAt().toString(),
            "updatedAt", note.getUpdatedAt().toString(),
            "isConfidential", note.getConfidential()
        );
    }

    private Map<String, Object> legalCaseView(LegalCaseRecord legalCase) {
        return mapOf(
            "id", legalCase.getId(),
            "victimId", legalCase.getVictimId(),
            "advisorId", legalCase.getAdvisorId(),
            "title", legalCase.getTitle(),
            "caseTitle", legalCase.getTitle(),
            "summary", stringValue(legalCase.getSummary(), ""),
            "description", stringValue(legalCase.getSummary(), ""),
            "caseType", stringValue(legalCase.getCaseType(), "OTHER"),
            "caseNumber", stringValue(legalCase.getCaseNumber(), ""),
            "status", legalCase.getStatus(),
            "createdAt", legalCase.getCreatedAt().toString(),
            "updatedAt", legalCase.getUpdatedAt().toString()
        );
    }

    private Map<String, Object> chatMessageView(ChatMessageRecord message) {
        return mapOf(
            "id", message.getId(),
            "senderId", message.getSenderId(),
            "receiverId", message.getReceiverId(),
            "sessionId", message.getSessionId(),
            "message", message.getMessage(),
            "timestamp", message.getTimestamp().toString()
        );
    }

    private Map<String, Object> chatSessionVictimView(ChatSessionRecord session) {
        UserAccount agent = userAccountRepository.findById(session.getAgentId()).orElse(null);
        ChatMessageRecord latestMessage = latestMessageForSession(session.getId());
        return mapOf(
            "id", session.getId(),
            "victimId", session.getVictimId(),
            "agentId", session.getAgentId(),
            "agentName", agent != null ? stringValue(agent.getName(), "Support Agent") : "Support Agent",
            "agentEmail", agent != null ? stringValue(agent.getEmail(), "") : "",
            "agentRole", agent != null ? normalizedRole(agent.getRole()) : "Support",
            "status", stringValue(session.getStatus(), "ACTIVE"),
            "createdAt", session.getCreatedAt().toString(),
            "updatedAt", session.getUpdatedAt().toString(),
            "lastMessage", latestMessage != null ? stringValue(latestMessage.getMessage(), "") : "",
            "lastMessageAt", latestMessage != null ? latestMessage.getTimestamp().toString() : session.getUpdatedAt().toString()
        );
    }

    private Map<String, Object> chatSessionThreadView(ChatSessionRecord session) {
        UserAccount victim = userAccountRepository.findById(session.getVictimId()).orElse(null);
        if (victim == null || !"Victim".equalsIgnoreCase(stringValue(victim.getRole(), ""))) {
            return null;
        }
        ChatMessageRecord latestMessage = latestMessageForSession(session.getId());
        return mapOf(
            "sessionId", session.getId(),
            "victimId", victim.getId(),
            "victimName", stringValue(victim.getName(), "Victim"),
            "victimEmail", stringValue(victim.getEmail(), ""),
            "status", stringValue(session.getStatus(), "ACTIVE"),
            "lastMessage", latestMessage != null ? stringValue(latestMessage.getMessage(), "") : "",
            "createdAt", session.getCreatedAt().toString(),
            "updatedAt", session.getUpdatedAt().toString(),
            "lastMessageAt", latestMessage != null ? latestMessage.getTimestamp().toString() : session.getUpdatedAt().toString()
        );
    }

    private void notifyAgentIfNeeded(Map<String, Object> message) {
        Integer senderId = parseInteger(message.get("senderId"));
        Integer receiverId = parseInteger(message.get("receiverId"));

        if (senderId == null || receiverId == null) {
            return;
        }

        UserAccount sender = userAccountRepository.findById(senderId.longValue()).orElse(null);
        UserAccount receiver = userAccountRepository.findById(receiverId.longValue()).orElse(null);

        if (sender == null || receiver == null) {
            return;
        }

        if (!"Victim".equalsIgnoreCase(sender.getRole())) {
            return;
        }

        if (!isAuthorizedSupportRole(receiver.getRole())) {
            return;
        }

        agentNotificationService.notifyAgentOfIncomingChat(sender, receiver, stringValue(message.get("message"), "New support chat message"));
    }

    private boolean isAuthorizedSupportRole(String role) {
        if (role == null) {
            return false;
        }
        return "Counsellor".equalsIgnoreCase(role)
            || "LegalAdvisor".equalsIgnoreCase(role)
            || "Admin".equalsIgnoreCase(role);
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private long requiredLong(Object value) {
        Long parsed = parseLong(value);
        if (parsed == null) {
            throw new IllegalArgumentException("Missing required numeric value");
        }
        return parsed;
    }

    private boolean parseBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value == null) {
            return fallback;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private LocalDateTime parseLocalDateTime(Object value) {
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        if (value == null) {
            return LocalDateTime.now();
        }
        String rawValue = value.toString();
        try {
            return LocalDateTime.parse(rawValue);
        } catch (DateTimeParseException ignored) {
            return Instant.parse(rawValue).atZone(ZoneOffset.UTC).toLocalDateTime();
        }
    }

    private String agentSpecialty(String role) {
        if (role == null) {
            return "General support";
        }
        return switch (role) {
            case "Counsellor" -> "Emotional support and trauma-informed guidance";
            case "LegalAdvisor" -> "Rights, legal options, and case guidance";
            case "Admin" -> "Platform safety coordination and escalation support";
            default -> "Support";
        };
    }

    private String normalizedRole(String role) {
        if (role == null || role.isBlank()) {
            return "Support";
        }
        if ("counsellor".equalsIgnoreCase(role)) {
            return "Counsellor";
        }
        if ("legaladvisor".equalsIgnoreCase(role) || "legal advisor".equalsIgnoreCase(role)) {
            return "LegalAdvisor";
        }
        if ("admin".equalsIgnoreCase(role)) {
            return "Admin";
        }
        return role;
    }

    private ChatMessageRecord latestMessageForSession(Long sessionId) {
        List<ChatMessageRecord> messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }

    private void touchSession(Long sessionId) {
        ChatSessionRecord session = chatSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return;
        }
        chatSessionRepository.save(session);
    }

    private void applyResourceRequest(ResourceRecord resource, Map<String, Object> request) {
        resource.setName(stringValue(request.get("name"), stringValue(request.get("title"), "Untitled Resource")));
        resource.setDescription(stringValue(request.get("description"), ""));
        resource.setResourceType(stringValue(request.get("resourceType"), stringValue(request.get("category"), "General")));
        resource.setTargetAudience(stringValue(request.get("targetAudience"), "All Users"));
        resource.setContactInfo(stringValue(request.get("contactInfo"), ""));
        resource.setEmergencyHotline(stringValue(request.get("emergencyHotline"), ""));
        Integer priority = parseInteger(request.get("priorityLevel"));
        resource.setPriorityLevel(priority != null ? priority : 5);
        resource.setActive(parseBoolean(request.get("isActive"), true));
    }

    private String buildCaseNumber() {
        long nextId = legalCaseRepository.count() + 1;
        return "SN-CASE-" + String.format("%04d", nextId);
    }

    private void seedResourcesIfEmpty() {
        if (resourceRepository.count() > 0) {
            return;
        }

        resourceRepository.save(createSeedResource("Emergency Contacts", "Local helplines and support numbers", "Emergency", "Victim,Counsellor", "181", "181", 10));
        resourceRepository.save(createSeedResource("Safety Planning Guide", "Build a personal safety plan with practical next steps.", "Safety", "Victim", "support@safeharbor.org", "112", 9));
        resourceRepository.save(createSeedResource("Legal Rights Overview", "Protection orders, FIR support, and rights awareness.", "Legal", "Victim,LegalAdvisor", "legal@safeharbor.org", "", 8));
        resourceRepository.save(createSeedResource("Counselling Resources", "Trauma-informed counselling and referral services.", "Counselling", "Victim,Counsellor", "care@safeharbor.org", "", 7));
    }

    private void seedOperationalDataIfEmpty() {
        UserAccount demoVictim = userAccountRepository.findByEmailIgnoreCase("victim@example.com").orElse(null);
        UserAccount demoCounsellor = userAccountRepository.findByEmailIgnoreCase("counsellor@example.com").orElse(null);
        UserAccount demoLegal = userAccountRepository.findByEmailIgnoreCase("legal@example.com").orElse(null);

        if (demoVictim == null || demoCounsellor == null || demoLegal == null) {
            return;
        }

        if (appointmentRepository.count() == 0) {
            appointmentRepository.save(seedAppointment(demoVictim.getId(), demoCounsellor.getId(), "VIDEO_CALL", LocalDateTime.of(2026, 4, 28, 14, 0), "SCHEDULED", "Initial assessment"));
            appointmentRepository.save(seedAppointment(demoVictim.getId(), demoCounsellor.getId(), "PHONE_CALL", LocalDateTime.of(2026, 4, 30, 10, 30), "COMPLETED", "Follow-up session"));
        }

        if (caseNoteRepository.count() == 0) {
            caseNoteRepository.save(seedCaseNote(demoVictim.getId(), demoCounsellor.getId(), "Victim reports ongoing harassment. Recommended safety plan and legal consultation.", "SESSION_NOTES", false));
            caseNoteRepository.save(seedCaseNote(demoVictim.getId(), demoCounsellor.getId(), "Victim reports improved sleep and stronger support network.", "PROGRESS", false));
        }

        if (legalCaseRepository.count() == 0) {
            legalCaseRepository.save(seedLegalCase(demoVictim.getId(), demoLegal.getId(), "Protection Order Support", "Preparing documents for a protection order filing.", "DOMESTIC_VIOLENCE", "IN_PROGRESS"));
            legalCaseRepository.save(seedLegalCase(demoVictim.getId(), demoLegal.getId(), "Police Complaint Guidance", "Reviewing complaint details and next legal steps.", "HARASSMENT", "OPEN"));
        }

        ChatSessionRecord demoSession = seedDemoChatSession(demoVictim.getId(), demoCounsellor.getId());
        seedDemoChatMessage(demoSession.getId(), demoVictim.getId(), demoCounsellor.getId(), "I need help scheduling another session.", Instant.now().minusSeconds(7200));
        seedDemoChatMessage(demoSession.getId(), demoCounsellor.getId(), demoVictim.getId(), "Absolutely. I can help you book one for tomorrow afternoon.", Instant.now().minusSeconds(6900));
    }

    private ResourceRecord createSeedResource(String name, String description, String resourceType, String targetAudience, String contactInfo, String emergencyHotline, int priorityLevel) {
        ResourceRecord resource = new ResourceRecord();
        resource.setName(name);
        resource.setDescription(description);
        resource.setResourceType(resourceType);
        resource.setTargetAudience(targetAudience);
        resource.setContactInfo(contactInfo);
        resource.setEmergencyHotline(emergencyHotline);
        resource.setPriorityLevel(priorityLevel);
        resource.setActive(true);
        return resource;
    }

    private AppointmentRecord seedAppointment(Long victimId, Long counsellorId, String type, LocalDateTime dateTime, String status, String notes) {
        AppointmentRecord appointment = new AppointmentRecord();
        appointment.setVictimId(victimId);
        appointment.setCounsellorId(counsellorId);
        appointment.setType(type);
        appointment.setAppointmentDateTime(dateTime);
        appointment.setStatus(status);
        appointment.setNotes(notes);
        return appointment;
    }

    private CaseNoteRecord seedCaseNote(Long victimId, Long counsellorId, String noteContent, String category, boolean confidential) {
        CaseNoteRecord note = new CaseNoteRecord();
        note.setVictimId(victimId);
        note.setCounsellorId(counsellorId);
        note.setNoteContent(noteContent);
        note.setCategory(category);
        note.setConfidential(confidential);
        return note;
    }

    private LegalCaseRecord seedLegalCase(Long victimId, Long advisorId, String title, String summary, String caseType, String status) {
        LegalCaseRecord legalCase = new LegalCaseRecord();
        legalCase.setVictimId(victimId);
        legalCase.setAdvisorId(advisorId);
        legalCase.setTitle(title);
        legalCase.setSummary(summary);
        legalCase.setCaseType(caseType);
        legalCase.setCaseNumber(buildCaseNumber());
        legalCase.setStatus(status);
        return legalCase;
    }

    private ChatSessionRecord seedDemoChatSession(Long victimId, Long agentId) {
        return chatSessionRepository.findByVictimIdOrderByUpdatedAtDesc(victimId).stream()
            .filter(session -> agentId.equals(session.getAgentId()))
            .findFirst()
            .orElseGet(() -> {
                ChatSessionRecord session = new ChatSessionRecord();
                session.setVictimId(victimId);
                session.setAgentId(agentId);
                session.setStatus("ACTIVE");
                return chatSessionRepository.save(session);
            });
    }

    private void seedDemoChatMessage(Long sessionId, Long senderId, Long receiverId, String message, Instant timestamp) {
        boolean exists = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId).stream()
            .anyMatch(item -> message.equals(item.getMessage()));

        if (exists) {
            return;
        }

        ChatMessageRecord record = new ChatMessageRecord();
        record.setSessionId(sessionId);
        record.setSenderId(senderId);
        record.setReceiverId(receiverId);
        record.setMessage(message);
        record.setTimestamp(timestamp);
        chatMessageRepository.save(record);
        touchSession(sessionId);
    }

    private void logUserActivity(UserAccount user, String actionType, String subjectType, Long subjectId, String description, String details) {
        logActivity(user.getId(), normalizedRole(user.getRole()), actionType, subjectType, subjectId, description, details, user.getName());
    }

    private void logActivity(Long actorUserId, String actorRole, String actionType, String subjectType, Long subjectId, String description, String details) {
        UserAccount actor = actorUserId != null ? userAccountRepository.findById(actorUserId).orElse(null) : null;
        String actorName = actor != null ? stringValue(actor.getName(), "System") : "System";
        String resolvedRole = actor != null ? normalizedRole(actor.getRole()) : actorRole;
        logActivity(actorUserId, resolvedRole, actionType, subjectType, subjectId, description, details, actorName);
    }

    private void logActivity(Long actorUserId, String actorRole, String actionType, String subjectType, Long subjectId, String description, String details, String actorName) {
        ActivityLogRecord activity = new ActivityLogRecord();
        activity.setActorUserId(actorUserId);
        activity.setActorName(stringValue(actorName, "System"));
        activity.setActorRole(stringValue(actorRole, "SYSTEM"));
        activity.setActionType(actionType);
        activity.setSubjectType(subjectType);
        activity.setSubjectId(subjectId);
        activity.setDescription(description);
        activity.setDetails(details);
        activityLogRepository.save(activity);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return stringValue(value, "");
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new HashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(values[index].toString(), values[index + 1]);
        }
        return map;
    }
}
