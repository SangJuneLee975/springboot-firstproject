import com.example.apitest.config.AuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthenticationInterceptor authenticationInterceptor;

    @Autowired
    public WebConfig(AuthenticationInterceptor authenticationInterceptor) {
        this.authenticationInterceptor = authenticationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 모든 URL에 인증 인터셉터를 등록합니다.
        registry.addInterceptor(authenticationInterceptor)
                .excludePathPatterns("/public/**"); // "/public/**" 패턴을 제외하고 모든 URL에 인증 인터셉터를 적용합니다.
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // 모든 경로에 대해
                        .allowedOrigins("http://localhost:3000") // 이 출처의 프론트엔드 요청을 허용
                        .allowedMethods("GET", "POST", "PUT", "DELETE") // 허용할 HTTP 메소드 지정
                        .allowCredentials(true);
            }
        };
    }
}
