package com.redseeker.diary;

import com.redseeker.common.ErrorCode;
import com.redseeker.common.ServiceException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class DiaryServiceImpl implements DiaryService {
  private final AtomicLong idGenerator = new AtomicLong(1);
  private final Map<Long, DiaryEntry> store = new ConcurrentHashMap<>();
  private final DiaryRepository repository = new DiaryRepository();

  @Override
  public DiaryEntry create(DiaryRequest request) {
    Long id = idGenerator.getAndIncrement();
    Instant now = Instant.now();
    Long userId = resolveUserId(request.getUserId());
    boolean isPublic = Boolean.TRUE.equals(request.getPublicEntry());
    DiaryEntry entry = new DiaryEntry(id, userId, request.getTitle(), request.getContent(),
        safeList(request.getImages()), request.getAttractionId(), isPublic, now, now);
    if (repository.isDatabaseReady()) {
      return repository.insert(entry);
    }
    store.put(id, entry);
    return entry;
  }

  @Override
  public DiaryEntry update(Long id, DiaryRequest request) {
    DiaryEntry existing = get(id);
    Instant now = Instant.now();
    Long userId = request.getUserId() != null ? request.getUserId() : existing.getUserId();
    boolean isPublic = request.getPublicEntry() != null ? request.getPublicEntry() : existing.isPublicEntry();
    DiaryEntry updated = new DiaryEntry(id,
        userId,
        request.getTitle() != null ? request.getTitle() : existing.getTitle(),
        request.getContent() != null ? request.getContent() : existing.getContent(),
        request.getImages() != null ? request.getImages() : existing.getImages(),
        request.getAttractionId() != null ? request.getAttractionId() : existing.getAttractionId(),
        isPublic,
        existing.getCreatedAt(),
        now);
    if (repository.isDatabaseReady()) {
      return repository.update(updated);
    }
    store.put(id, updated);
    return updated;
  }

  @Override
  public DiaryEntry get(Long id) {
    if (repository.isDatabaseReady()) {
      return repository.fetchById(id)
          .orElseThrow(() -> new ServiceException(ErrorCode.NOT_FOUND, "日志不存在"));
    }
    DiaryEntry entry = store.get(id);
    if (entry == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "日志不存在");
    }
    return entry;
  }

  @Override
  public List<DiaryEntry> list() {
    if (repository.isDatabaseReady()) {
      return repository.list();
    }
    return store.values().stream()
        .sorted(Comparator.comparing(DiaryEntry::getCreatedAt).reversed())
        .toList();
  }

  @Override
  public DiaryEntry share(Long id) {
    DiaryEntry entry = get(id);
    DiaryEntry shared = new DiaryEntry(entry.getId(), entry.getUserId(), entry.getTitle(), entry.getContent(),
        entry.getImages(), entry.getAttractionId(), true, entry.getCreatedAt(), Instant.now());
    if (repository.isDatabaseReady()) {
      return repository.update(shared);
    }
    store.put(id, shared);
    return shared;
  }

  private List<String> safeList(List<String> input) {
    return input == null ? new ArrayList<>() : new ArrayList<>(input);
  }

  private Long resolveUserId(Long requestedUserId) {
    return requestedUserId != null ? requestedUserId : 1L;
  }
}
