package nbc.chillguys.nebulazone.support.MockMvc;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import nbc.chillguys.nebulazone.application.ban.sevice.BanService;

@TestConfiguration
public class TestMockConfig {

	@Bean
	public BanService banService() {
		return Mockito.mock(BanService.class);
	}
}
