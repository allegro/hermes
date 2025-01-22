package pl.allegro.tech.hermes.domain.notifications;

public interface AdminCallback {

  void onAdminOperationCreated(String type, String content);
}
