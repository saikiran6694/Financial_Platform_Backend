package com.arthium.finance.user;

import com.arthium.finance.common.ApiException;
import com.arthium.finance.common.Ids;
import com.arthium.finance.storage.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public UserService(UserRepository userRepository, CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
    }

    public User getById(String userId) {
        if (!Ids.isValid(userId)) {
            throw ApiException.notFound("User not found");
        }
        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    public User updateUser(String userId, MultipartFile profilePicture, String name) {
        User user = getById(userId);

        boolean changed = false;

        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                String url = cloudinaryService.uploadFile(
                        profilePicture.getBytes(),
                        profilePicture.getOriginalFilename(),
                        "avatars");
                user.setProfilePicture(url);
                changed = true;
            } catch (IOException e) {
                throw ApiException.badRequest("Unable to read the uploaded file");
            }
        }

        if (name != null) {
            user.setName(name);
            changed = true;
        }

        if (!changed) {
            throw ApiException.badRequest("No fields to update");
        }

        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }
}
