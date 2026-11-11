#include <sys/socket.h>
#include <sys/un.h>
#include <sys/uio.h>
#include <unistd.h>
#include <fcntl.h>
#include <dirent.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cstdint>

namespace off {
    constexpr uintptr_t kAimLegitRva    = 0x1A4C890;
    constexpr uintptr_t kAimTrickRva    = 0x1A4D114;
    constexpr uintptr_t kHeadTrickRva   = 0x1B90320;
    constexpr uintptr_t kSmoothAimRva   = 0x1A4C9F0;
    constexpr uintptr_t kSensMultiplier = 0x1D04410;
    constexpr uintptr_t kMcGraphicsLod  = 0x21108A0;
}

struct IpcPacket {
    uint8_t cmd;
    int32_t value;
};

static pid_t find_ff_pid() noexcept {
    DIR* d = opendir("/proc");
    if (!d) return -1;
    struct dirent* e = nullptr;
    while ((e = readdir(d)) != nullptr) {
        pid_t pid = std::atoi(e->d_name);
        if (pid <= 0) continue;
        char path[64]{};
        std::snprintf(path, sizeof(path), "/proc/%d/cmdline", pid);
        int fd = open(path, O_RDONLY);
        if (fd < 0) continue;
        char cmd[128]{};
        ssize_t n = read(fd, cmd, sizeof(cmd) - 1);
        close(fd);
        if (n > 0 && (std::strstr(cmd, "com.dts.freefireth") || std::strstr(cmd, "com.dts.freefiremax"))) {
            closedir(d);
            return pid;
        }
    }
    closedir(d);
    return -1;
}

static uintptr_t find_il2cpp_base(pid_t pid) noexcept {
    char path[64]{};
    std::snprintf(path, sizeof(path), "/proc/%d/maps", pid);
    FILE* fp = std::fopen(path, "r");
    if (!fp) return 0;
    char line[512]{};
    uintptr_t base = 0;
    while (std::fgets(line, sizeof(line), fp)) {
        if (std::strstr(line, "libil2cpp.so")) {
            base = std::strtoull(line, nullptr, 16);
            break;
        }
    }
    std::fclose(fp);
    return base;
}

template <typename T>
static bool pvm_write(pid_t pid, uintptr_t addr, const T& val) noexcept {
    if (pid <= 0 || addr < 0x10000) return false;
    T buf = val;
    struct iovec local{&buf, sizeof(T)};
    struct iovec remote{reinterpret_cast<void*>(addr), sizeof(T)};
    return process_vm_writev(pid, &local, 1, &remote, 1, 0) == static_cast<ssize_t>(sizeof(T));
}

static void exec_adb_prop(const char* cmd) noexcept {
    std::system(cmd);
}

static void apply_command(const IpcPacket& pkt) noexcept {
    pid_t pid = find_ff_pid();
    uintptr_t base = (pid > 0) ? find_il2cpp_base(pid) : 0;

    switch (pkt.cmd) {
        case 1: {
            exec_adb_prop(pkt.value ? "settings put secure tap_duration_threshold 0.0" : "settings delete secure tap_duration_threshold");
            if (base) {
                const uint64_t p = pkt.value ? 0xD65F03C01E2E9000ULL : 0xD65F03C01E2703E0ULL;
                pvm_write(pid, base + off::kAimLegitRva, p);
            }
            break;
        }
        case 2: {
            exec_adb_prop(pkt.value ? "setprop debug.touch.velocity_scale 1.65" : "setprop debug.touch.velocity_scale 1.0");
            if (base) {
                const float v = pkt.value ? 0.15f : 1.0f;
                pvm_write(pid, base + off::kAimTrickRva, v);
            }
            break;
        }
        case 3: {
            exec_adb_prop(pkt.value ? "setprop debug.input.y_scale 1.85" : "setprop debug.input.y_scale 1.0");
            if (base) {
                const float v = pkt.value ? 1.85f : 1.0f;
                pvm_write(pid, base + off::kHeadTrickRva, v);
            }
            break;
        }
        case 4: {
            exec_adb_prop(pkt.value ? "setprop debug.sf.latch_unsignaled 1" : "setprop debug.sf.latch_unsignaled 0");
            if (base) {
                const float v = pkt.value ? 0.42f : 1.0f;
                pvm_write(pid, base + off::kSmoothAimRva, v);
            }
            break;
        }
        case 5: {
            int lvl = (pkt.value < 1) ? 1 : (pkt.value > 7 ? 7 : pkt.value);
            char cmd[128]{};
            std::snprintf(cmd, sizeof(cmd), "settings put system pointer_speed %d", lvl);
            exec_adb_prop(cmd);
            if (base) {
                const float scale = 1.0f + (static_cast<float>(lvl - 1) * 0.30f);
                pvm_write(pid, base + off::kSensMultiplier, scale);
            }
            break;
        }
        case 6: {
            if (pkt.value) {
                exec_adb_prop("cmd game mode performance com.dts.freefireth");
                exec_adb_prop("wm size 540x1200");
                exec_adb_prop("setprop debug.hwui.filter_test_overhead true");
            } else {
                exec_adb_prop("wm size reset");
                exec_adb_prop("setprop debug.hwui.filter_test_overhead false");
            }
            if (base) {
                const float mip = pkt.value ? 8.0f : 0.0f;
                pvm_write(pid, base + off::kMcGraphicsLod, mip);
            }
            break;
        }
        case 7: {
            if (pkt.value) {
                exec_adb_prop("echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
                exec_adb_prop("echo performance > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor");
                exec_adb_prop("cmd activity compact");
                exec_adb_prop("setprop debug.perf.gpulock 1");
            } else {
                exec_adb_prop("echo schedutil > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
                exec_adb_prop("echo schedutil > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor");
                exec_adb_prop("setprop debug.perf.gpulock 0");
            }
            break;
        }
        default:
            break;
    }
}

int main() {
    int srv = socket(AF_UNIX, SOCK_STREAM, 0);
    if (srv < 0) return 1;

    struct sockaddr_un addr{};
    addr.sun_family = AF_UNIX;
    std::strncpy(addr.sun_path + 1, "danzstraight_ipc", sizeof(addr.sun_path) - 2);
    socklen_t len = sizeof(sa_family_t) + 1 + std::strlen("danzstraight_ipc");

    if (bind(srv, reinterpret_cast<struct sockaddr*>(&addr), len) < 0) {
        close(srv);
        return 1;
    }
    listen(srv, 4);

    while (true) {
        int cli = accept(srv, nullptr, nullptr);
        if (cli < 0) continue;
        IpcPacket pkt{};
        if (read(cli, &pkt, sizeof(pkt)) == sizeof(pkt)) {
            if (pkt.cmd == 99) {
                close(cli);
                break;
            }
            apply_command(pkt);
        }
        close(cli);
    }

    close(srv);
    return 0;
}
