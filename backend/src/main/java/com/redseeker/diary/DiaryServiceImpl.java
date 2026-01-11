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

  @Override
  public DiaryEntry create(DiaryRequest request) {
    Long id = idGenerator.getAndIncrement();
    Instant now = Instant.now();
    String template = resolveTemplate(request.getTemplate(), request.getAttractionId());
    DiaryEntry entry = new DiaryEntry(id, request.getTitle(), request.getContent(),
        safeList(request.getImages()), safeList(request.getTags()), request.getAttractionId(),
        Boolean.TRUE.equals(request.getCheckedIn()), request.getCheckInNote(), template, false, now, now);
    store.put(id, entry);
    return entry;
  }

  @Override
  public DiaryEntry update(Long id, DiaryRequest request) {
    DiaryEntry existing = get(id);
    Instant now = Instant.now();
    String template = resolveTemplate(request.getTemplate(), request.getAttractionId());
    DiaryEntry updated = new DiaryEntry(id,
        request.getTitle() != null ? request.getTitle() : existing.getTitle(),
        request.getContent() != null ? request.getContent() : existing.getContent(),
        request.getImages() != null ? request.getImages() : existing.getImages(),
        request.getTags() != null ? request.getTags() : existing.getTags(),
        request.getAttractionId() != null ? request.getAttractionId() : existing.getAttractionId(),
        request.getCheckedIn() != null ? request.getCheckedIn() : existing.isCheckedIn(),
        request.getCheckInNote() != null ? request.getCheckInNote() : existing.getCheckInNote(),
        template != null ? template : existing.getTemplate(),
        existing.isShared(),
        existing.getCreatedAt(),
        now);
    store.put(id, updated);
    return updated;
  }

  @Override
  public DiaryEntry get(Long id) {
    DiaryEntry entry = store.get(id);
    if (entry == null) {
      throw new ServiceException(ErrorCode.NOT_FOUND, "日志不存在");
    }
    return entry;
  }

  @Override
  public List<DiaryEntry> list() {
    return store.values().stream()
        .sorted(Comparator.comparing(DiaryEntry::getCreatedAt).reversed())
        .toList();
  }

  @Override
  public DiaryEntry share(Long id) {
    DiaryEntry entry = get(id);
    DiaryEntry shared = new DiaryEntry(entry.getId(), entry.getTitle(), entry.getContent(), entry.getImages(),
        entry.getTags(), entry.getAttractionId(), entry.isCheckedIn(), entry.getCheckInNote(), entry.getTemplate(),
        true, entry.getCreatedAt(), Instant.now());
    store.put(id, shared);
    return shared;
  }

  private List<String> safeList(List<String> input) {
    return input == null ? new ArrayList<>() : new ArrayList<>(input);
  }

  private String resolveTemplate(String template, Long attractionId) {
    if (template != null && !template.isBlank()) {
      return template;
    }
    if (attractionId == null) {
      return "标题\\n- 今日研学主题\\n- 重点收获\\n- 心得体会";
    }
    return "景点记录" + attractionId + "\\n- 历史脉络\\n- 讲解要点\\n- 打卡感想\\n- 后续行动";
  }
}
