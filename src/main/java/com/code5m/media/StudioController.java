package com.code5m.media;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1")
public class StudioController {
  public record Platform(String id,String name,String mode,String verification) {}
  public record Target(String platform,String accountId) {}
  public record PublishRequest(String contentId,List<Target> targets,String scheduleAt,boolean dryRun) {}
  public record Job(String platform,String accountId,String mode,String status,String reason) {}
  public record Batch(String id,String status,boolean dryRun,List<Job> jobs) {}
  private static final List<Platform> PLATFORMS=List.of(
    new Platform("wechat","微信公众号","DIRECT_API","DOCUMENTED"),
    new Platform("xiaohongshu","小红书","HANDOFF","DOCUMENTED"),
    new Platform("douyin","抖音","DIRECT_API","DOCUMENTED"),
    new Platform("bilibili","B站","HANDOFF","DOCUMENTED"),
    new Platform("weibo","微博","HANDOFF","DOCUMENTED"),
    new Platform("zhihu","知乎","HANDOFF","DOCUMENTED"),
    new Platform("wordpress","WordPress","DIRECT_API","DOCUMENTED"));
  private final Map<String,Batch> batches=new java.util.concurrent.ConcurrentHashMap<>();
  private final Map<String,String> idempotency=new java.util.concurrent.ConcurrentHashMap<>();
  @GetMapping("/health") public Map<String,Object> health(){return Map.of("status","UP","runtime","java","mode","scaffold");}
  @GetMapping("/platforms") public List<Platform> platforms(){return PLATFORMS;}
  @PostMapping("/publish-batches")
  public ResponseEntity<?> publish(@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody PublishRequest request){
    if(request.contentId()==null||request.contentId().isBlank()||request.targets()==null||request.targets().isEmpty())return ResponseEntity.badRequest().body(Map.of("code","INVALID_REQUEST","message","contentId and targets are required"));
    if(!request.dryRun())return ResponseEntity.status(501).body(Map.of("code","PUBLISH_NOT_IMPLEMENTED","message","Real publishing and authorization have not been implemented"));
    String id=key==null||key.isBlank()?UUID.randomUUID().toString():idempotency.computeIfAbsent(key,k->UUID.randomUUID().toString());
    Batch batch=batches.computeIfAbsent(id,k->new Batch(k,"VALIDATED",true,request.targets().stream().map(t->new Job(t.platform(),t.accountId(),"xiaohongshu".equals(t.platform())?"HANDOFF":"UNVERIFIED","BLOCKED_AUTH","No real platform authorization or media upload has been configured")).toList()));
    return ResponseEntity.accepted().body(batch);
  }
  @GetMapping("/publish-batches/{id}") public ResponseEntity<?> getBatch(@PathVariable String id){Batch batch=batches.get(id);return batch==null?ResponseEntity.notFound().build():ResponseEntity.ok(batch);}
}