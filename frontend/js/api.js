/* =============================================================================
 * 백엔드 API 호출 공통 함수.
 *
 * 같은 origin(프론트 포트)으로 /api/* 호출 → nginx가 backend 컨테이너로 프록시.
 * fetch 응답을 JSON으로 파싱하고, 에러 코드를 호출자에게 그대로 전달한다.
 * 네트워크 오류는 status:0 + 안내 메시지로 정규화하여 호출자가 분기할 수 있게 한다.
 * ============================================================================= */

const API_BASE = "/api";

/**
 * 공통 fetch 래퍼.
 * @param {string} path  /students, /students/1 등
 * @param {object} options  { method, body }  body는 객체 (자동 JSON.stringify)
 * @returns {Promise<{status:number, data:any}>}
 *   - 정상 응답: { status: 200~5xx, data: 파싱된 JSON / 텍스트 / null }
 *   - 네트워크 오류(서버 다운, DNS 실패 등): { status: 0, data: { message } }
 */
async function request(path, options = {}) {
  const config = {
    method: options.method || "GET",
    headers: { "Content-Type": "application/json" },
  };
  if (options.body !== undefined) {
    config.body = JSON.stringify(options.body);
  }

  let res;
  try {
    res = await fetch(API_BASE + path, config);
  } catch (err) {
    // 네트워크 실패 — 백엔드 다운/방화벽/DNS 등. throw 하지 않고 정규화된 응답으로 변환.
    console.error("API 네트워크 오류:", err);
    return {
      status: 0,
      data: { message: "서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요." },
    };
  }

  // 204 등 본문 없는 응답에도 안전하게 동작
  let data = null;
  const text = await res.text();
  if (text) {
    try { data = JSON.parse(text); }
    catch { data = text; }
  }
  return { status: res.status, data };
}

// 학생 -----------------------------------------------------------------------
const api = {
  listStudents:        ()        => request("/students"),
  getStudent:          (id)      => request("/students/" + id),
  createStudent:       (body)    => request("/students", { method: "POST", body }),
  updateStudent:       (id, b)   => request("/students/" + id, { method: "PUT", body: b }),
  deleteStudent:       (id)      => request("/students/" + id, { method: "DELETE" }),
  getStudentScores:    (id)      => request("/students/" + id + "/scores"),

  // 성적
  createScore:         (body)    => request("/scores", { method: "POST", body }),
  updateScore:         (id, b)   => request("/scores/" + id, { method: "PUT", body: b }),
  deleteScore:         (id)      => request("/scores/" + id, { method: "DELETE" }),

  // 조회
  getReport:           (id)      => request("/reports/" + id),
  listSubjects:        ()        => request("/subjects"),
};

/**
 * 메시지 표시 헬퍼.
 * 각 페이지 상단에 <div id="msg" class="message"></div> 가 있다고 가정.
 */
function showMessage(text, type = "error") {
  const el = document.getElementById("msg");
  if (!el) return;
  el.textContent = text;
  el.className = "message " + type;
  el.style.display = "block";
  if (type === "success") {
    setTimeout(() => { el.style.display = "none"; }, 3000);
  }
}

function clearMessage() {
  const el = document.getElementById("msg");
  if (el) { el.style.display = "none"; el.textContent = ""; }
}
