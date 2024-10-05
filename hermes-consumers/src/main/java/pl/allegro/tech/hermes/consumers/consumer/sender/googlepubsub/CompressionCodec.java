package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

enum CompressionCodec {
  DEFLATE("df"),
  BZIP2("bz2"),
  ZSTANDARD("zstd"),
  EMPTY;

  private String header;

  CompressionCodec(String header) {
    this.header = header;
  }

  CompressionCodec() {}

  String getHeader() {
    return header;
  }
}
