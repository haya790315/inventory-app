package inventory.example.inventory_id.service;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenCacheService {

  private static final Logger logger = LoggerFactory.getLogger(
      TokenCacheService.class);

  @Autowired
  private RedisTemplate<String, Object> redisTemplate;

  private static final String SESSION_PREFIX = "user_session:";

  @Value("${spring.cache.redis.time-to-live:5m}")
  private Duration SESSION_TIMEOUT;

  /**
   * 認証成功後にユーザーをキャッシュする
   *
   * @param token  認証トークン (JWT/Firebaseトークン)
   * @param userId キャッシュするユーザーID
   */
  public void cacheUser(String token, String userId) {
    String sessionKey = SESSION_PREFIX + token;
    try {
      redisTemplate.opsForValue().set(sessionKey, userId, SESSION_TIMEOUT);
      logger.info("ユーザーIDをRedisにキャッシュしました");
    } catch (Exception e) {
      logger.error("Redis キャッシュエラー: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * キャッシュからユーザーIDを取得する
   *
   * @param token 認証トークン
   * @return 見つかった場合はユーザーID、見つからない場合はnull
   */
  public String getUserIdFromCache(String token) {
    String sessionKey = SESSION_PREFIX + token;
    try {
      Object userId = redisTemplate.opsForValue().get(sessionKey);

      if (userId != null) {
        logger.info(
            "RedisからユーザーIDを取得しました (キャッシュヒット)");
        return userId.toString();
      } else {
        logger.info(
            "RedisにユーザーIDが見つかりませんでした (キャッシュミス)");
        return null;
      }
    } catch (Exception e) {
      logger.error("Redis 読み取りエラー: {}", e.getMessage(), e);
      return null; // Fall back to null if Redis fails
    }
  }

  /**
   * キャッシュからユーザーキャッシュを削除する (ログアウト)
   *
   * @param token 認証トークン
   */
  public void removeUserCache(String token) {
    String sessionKey = SESSION_PREFIX + token;
    Boolean deleted = redisTemplate.delete(sessionKey);
    logger.info(
        "RedisからユーザーIDを削除しました: deleted={}",
        deleted);
  }

  /**
   * キャッシュのタイムアウトを延長する
   *
   * @param token 認証トークン
   */
  public void refreshUserCache(String token) {
    String sessionKey = SESSION_PREFIX + token;
    Boolean refreshed = redisTemplate.expire(sessionKey, SESSION_TIMEOUT);
    logger.info(
        "ユーザーIDキャッシュタイムアウトを延長しました: refreshed={}",
        refreshed);
  }

  /**
   * トークンが存在するかチェックする
   *
   * @param token 認証トークン
   * @return キャッシュが存在する場合true、存在しない場合false
   */
  public boolean isTokenCached(String token) {
    String sessionKey = SESSION_PREFIX + token;
    return redisTemplate.hasKey(sessionKey);
  }
}
