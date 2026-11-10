#include <jni.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <cstring>
#include <cstdint>

struct IpcPacket {
    uint8_t cmd;
    int32_t value;
};

static bool send_to_shizuku_daemon(uint8_t cmd, int32_t value) noexcept {
    int fd = socket(AF_UNIX, SOCK_STREAM, 0);
    if (fd < 0) return false;

    struct sockaddr_un addr{};
    addr.sun_family = AF_UNIX;
    std::strncpy(addr.sun_path + 1, "danzstraight_ipc", sizeof(addr.sun_path) - 2);
    socklen_t len = sizeof(sa_family_t) + 1 + std::strlen("danzstraight_ipc");

    if (connect(fd, reinterpret_cast<struct sockaddr*>(&addr), len) < 0) {
        close(fd);
        return false;
    }

    IpcPacket pkt{cmd, value};
    bool ok = (write(fd, &pkt, sizeof(pkt)) == sizeof(pkt));
    close(fd);
    return ok;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_danz_straight_NativeEngine_sendCommand(JNIEnv*, jclass, jint cmd, jint value) {
    return send_to_shizuku_daemon(static_cast<uint8_t>(cmd), static_cast<int32_t>(value)) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
