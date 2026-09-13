package com.leetjourney.user_service;

import com.leetjourney.user_service.entity.User;
import com.leetjourney.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class UserServiceApplicationTests {

	@Autowired
	private UserRepository userRepository;

	public static final int NUMBER_OF_USERS=15;
	@Test
	void contextLoads() {
	}

	@Disabled
	@Test
   void addUsersToDB(){

		for(int i=1;i<= NUMBER_OF_USERS; i++){
			User user=User.builder()
					.name("User"+i)
					.surname("Surname"+i)
					.email("email"+i)
					.address("address"+i)
					.alerting(i%2==0)
					.energyAlertingThreshold(1000.0+i)
					.build();
			userRepository.save(user);
		}

      log.info("User Repository populated successfully");
   }

}
