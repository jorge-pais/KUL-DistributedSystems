package be.kuleuven.distributedsystems.cloud.auth;

import be.kuleuven.distributedsystems.cloud.Application;
import be.kuleuven.distributedsystems.cloud.entities.Train;
import be.kuleuven.distributedsystems.cloud.entities.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.cloud.firestore.Firestore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.CollectionModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.Map;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Resource(name = "webClientBuilder")
    private WebClient.Builder webClientBuilder;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // TODO: (level 2) verify Identity Token

        String idToken = request.getHeader("Authorization");
        if(idToken == null) return;

        // TODO: PERGUNTAR SE ESTA MERDA É IMPORTANTE
        idToken = idToken.substring(7); // JANK way to remove the Bearer from the string

        String email = "no_email";
        String [] roles = new String[]{};

        try{
            if(!Application.isProduction()) {
            //if(false){
                DecodedJWT jwt = JWT.decode(idToken);
                email = jwt.getClaim("email").asString();
                roles = jwt.getClaim("roles").asArray(String.class);
            }else{
                ///read public key from google
                Map<String, String> keys =  webClientBuilder
                        .baseUrl("https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
                        .build()
                        .get()
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, String>>(){})
                        .block();

                String kid = JWT.decode(idToken).getKeyId();
                //System.out.println(kid);
                X509Certificate certificate = parseCertificate(keys.get(kid));

                RSAPublicKey pubKey = (RSAPublicKey) certificate.getPublicKey();

                Algorithm algorithm = Algorithm.RSA256(pubKey, null);
                DecodedJWT jwt = JWT.require(algorithm)
                        .withIssuer("https://securetoken.google.com/" + Application.projectId())
                        .build().verify(idToken);

                email = jwt.getClaim("email").asString();
                roles = jwt.getClaim("roles").asArray(String.class);
            }
        } catch (JWTDecodeException s){
            System.out.println("unauthorized!");
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("Something went really bad");
        }

        var user = new User(email, roles);
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(new FirebaseAuthentication(user));

        filterChain.doFilter(request, response);
    }

    protected X509Certificate parseCertificate(String cert) throws CertificateException {

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream stream = new ByteArrayInputStream(cert.getBytes());

        return (X509Certificate) cf.generateCertificate(stream);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.startsWith("/api");
    }

    public static User getUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private static class FirebaseAuthentication implements Authentication {
        private final User user;

        FirebaseAuthentication(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public User getPrincipal() {
            return this.user;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
        }

        @Override
        public void setAuthenticated(boolean b) throws IllegalArgumentException {

        }

        @Override
        public String getName() {
            return null;
        }
    }
}

