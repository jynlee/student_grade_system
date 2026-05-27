/* =============================================================================
 * 백엔드 API 호출 공통 함수.
 *
 * 백엔드 컨테이너는 포트 8081 에서 동작 (Docker Compose 설정).
 * fetch 응답을 JSON 으로 파싱하고, 에러 코드를 호출자에게 그대로 전달한다.
 * ============================================================================= */

const API_BASE = "http://localhost:8081/api";

/**
 * 공통 fetch 래퍼.
 * @param {string} path  /students, /students/1 등
 * @param {object} options  { method, body }  body는 객체 (자동 JSON.stringify)
 * @returns {Promise<{status:number, data:any}>}
 */
async function request(path, options = {}) {
  const config = {
    method: options.method || "GET",
    headers: { "Content-Type": "application/json" },
  };
  if (options.body !== undefined) {
    config.body = JSON.stringify(options.body);
  }
  const res = await fetch(API_BASE + path, config);
  let data = null;
  // 204 등 본문 없는 응답에도 안전하게 동작
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
