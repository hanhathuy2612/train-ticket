package com.example.userservice.service;

import com.example.userservice.dto.AuthResponse;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.RegisterRequest;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByUsername(request.getUsername())) {
			throw new RuntimeException("Username already exists");
		}

		if (userRepository.existsByEmail(request.getEmail())) {
			throw new RuntimeException("Email already exists");
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setFullName(request.getFullName());
		user.setPhoneNumber(request.getPhoneNumber());

		user = userRepository.save(user);

		String token = jwtUtil.generateToken(user.getUsername(), user.getId());
		
		// Cache user session in Redis
		redisTemplate.opsForValue().set("user:session:" + user.getId(), token, 24, TimeUnit.HOURS);

		return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
	}

	public AuthResponse login(LoginRequest request) {
		Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

		if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
			throw new RuntimeException("Invalid username or password");
		}

		User user = userOpt.get();

		if (!user.getActive()) {
			throw new RuntimeException("User account is inactive");
		}

		String token = jwtUtil.generateToken(user.getUsername(), user.getId());
		
		// Cache user session in Redis
		redisTemplate.opsForValue().set("user:session:" + user.getId(), token, 24, TimeUnit.HOURS);

		return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
	}

	public Optional<User> getUserById(Long id) {
		// Try to get from cache first
		String cacheKey = "user:" + id;
		User cachedUser = (User) redisTemplate.opsForValue().get(cacheKey);
		if (cachedUser != null) {
			return Optional.of(cachedUser);
		}

		Optional<User> user = userRepository.findById(id);
		user.ifPresent(u -> redisTemplate.opsForValue().set(cacheKey, u, 1, TimeUnit.HOURS));
		return user;
	}

	public Optional<User> getUserByUsername(String username) {
		return userRepository.findByUsername(username);
	}
}

