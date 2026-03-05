import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import html2canvas from 'html2canvas';
import {
  Smile, Frown, Meh, Angry, Zap,
  Calendar as CalendarIcon, BarChart2, Share2, ChevronLeft, ChevronRight,
  CheckCircle, LogOut, Loader2, Send, Home, User, Edit2, X, Star, Clock, AlertCircle,
  TrendingUp, Activity, Download, Quote, Trash2
} from 'lucide-react';
import joyImg from './assets/joy.png';
import calmImg from './assets/calm.png';
import sadnessImg from './assets/sadness.png';
import angerImg from './assets/anger.png';
import anxietyImg from './assets/anxiety.png';
import campusBg from './assets/campus-bg.png';
/**
 * [업데이트 내역]
 * 1. 로그인: 카카오 제거, 구글 단독 지원
 * 2. 통계 탭: 삭제 (캘린더에 기능 통합)
 * 3. 공유 기능: 'Emotion Recap' 스타일 (9:16 비율 카드) UI 구현
 * 4. 로직: 월간 통계 계산 로직을 프론트엔드 컴포넌트 내부에 구현 (Method A 적용)
 * 5. 간편 기록 개선: 감정 선택 시 강도 선택창이 그리드 하단에 고정되어 표시됨
 * 6. 삭제 기능: 상세 기록 모달에서 휴지통 아이콘을 통해 기록 삭제 가능 (확인 모달 포함)
 * 7. 상세 기록 개선: 감정 종류 수정 가능, 강도 조절을 5단계 버튼으로 변경
 * 8. 솔루션 & 피드백: 상세 모달 내 인라인 솔루션 표시 및 별점 평가 기능 추가
 * 9. 상세 기록 UX 개선 (New): 저장 버튼과 AI 요청 버튼 분리, 솔루션 영역 고정 UI 적용
 */
// --- API Configuration ---
const API_BASE = '/api/v1';
const GOOGLE_CLIENT_ID = '977745517550-qck18t5ns3ujsl1kbc3r8afa31t9og5s.apps.googleusercontent.com';

const getToken = () => localStorage.getItem('accessToken');
const setTokens = (access, refresh) => {
  localStorage.setItem('accessToken', access);
  if (refresh) localStorage.setItem('refreshToken', refresh);
};
const clearTokens = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
};

const api = async (path, options = {}) => {
  const token = getToken();
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
      ...options.headers,
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `API Error ${res.status}`);
  }
  return res.json();
};
// --- Constants & Types ---
const EMOTIONS = [
  { id: 'joy', label: '기쁨', icon: Smile, img: joyImg, color: 'bg-yellow-400', text: 'text-yellow-600', ring: 'ring-yellow-400', bgSoft: 'bg-yellow-50', gradient: 'from-yellow-50 to-orange-50' },
  { id: 'calm', label: '평온', icon: Meh, img: calmImg, color: 'bg-green-400', text: 'text-green-600', ring: 'ring-green-400', bgSoft: 'bg-green-50', gradient: 'from-green-50 to-teal-50' },
  { id: 'sadness', label: '슬픔', icon: Frown, img: sadnessImg, color: 'bg-blue-400', text: 'text-blue-600', ring: 'ring-blue-400', bgSoft: 'bg-blue-50', gradient: 'from-blue-50 to-indigo-50' },
  { id: 'anger', label: '화남', icon: Angry, img: angerImg, color: 'bg-red-400', text: 'text-red-600', ring: 'ring-red-400', bgSoft: 'bg-red-50', gradient: 'from-red-50 to-pink-50' },
  { id: 'anxiety', label: '긴장', icon: Zap, img: anxietyImg, color: 'bg-purple-400', text: 'text-purple-600', ring: 'ring-purple-400', bgSoft: 'bg-purple-50', gradient: 'from-purple-50 to-violet-50' },
];

const MOCK_SOLUTION = "잠시 하던 일을 멈추고 3분만 눈을 감아보세요. 지금 느끼는 감정은 지나가는 구름과 같습니다. 스스로를 다그치지 말고 있는 그대로 받아들이는 연습이 필요해요.";

// --- Helper Functions ---
const formatDate = (date) => {
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

// --- Basic Components ---
const Button = ({ children, onClick, variant = 'primary', className = '', disabled = false, icon: Icon }) => {
  const baseStyle = "w-full py-3.5 rounded-xl font-bold transition-all duration-200 active:scale-95 flex items-center justify-center gap-2 text-sm";
  const variants = {
    primary: "bg-indigo-600 text-white hover:bg-indigo-700 shadow-lg shadow-indigo-200 disabled:bg-gray-300 disabled:shadow-none",
    secondary: "bg-white text-gray-800 border border-gray-200 hover:bg-gray-50",
    google: "bg-white text-gray-700 border border-gray-300 hover:bg-gray-50 relative",
    danger: "bg-red-50 text-red-600 hover:bg-red-100",
  };
  return (
    <button onClick={onClick} disabled={disabled} className={`${baseStyle} ${variants[variant]} ${className}`}>
      {Icon && <Icon className="w-4 h-4" />}
      {children}
    </button>
  );
};

const Toast = ({ show, message, type = 'success', onAction, actionLabel, onClose }) => {
  useEffect(() => {
    if (show) {
      const timer = setTimeout(onClose, 4000);
      return () => clearTimeout(timer);
    }
  }, [show, onClose]);

  if (!show) return null;

  return (
    // z-50 -> z-[70]으로 수정하여 모달(z-50)보다 위에 표시되도록 함
    <div className="absolute bottom-24 left-4 right-4 z-[70] animate-in slide-in-from-bottom-5 duration-300">
      <div className="bg-gray-900 text-white px-4 py-3 rounded-xl shadow-2xl flex items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          {type === 'success' ? <CheckCircle className="w-5 h-5 text-green-400" /> : <AlertCircle className="w-5 h-5 text-yellow-400" />}
          <span className="text-sm font-medium">{message}</span>
        </div>
        {onAction && (
          <button
            onClick={onAction}
            className="text-xs font-bold text-indigo-300 hover:text-indigo-200 whitespace-nowrap px-2 py-1 bg-gray-800 rounded-lg border border-gray-700"
          >
            {actionLabel} &gt;
          </button>
        )}
      </div>
    </div>
  );
};

// --- Navigation ---
const BottomNav = ({ currentView, onChangeView }) => {
  const navItems = [
    { id: 'home', label: '홈', icon: Home },
    { id: 'calendar', label: '캘린더', icon: CalendarIcon },
  ];

  return (
    <nav className="absolute bottom-0 left-0 right-0 bg-white border-t border-gray-100 h-[80px] flex items-start justify-around px-8 pt-2 z-40 pb-safe">
      {navItems.map(item => {
        const isActive = currentView === item.id;
        return (
          <button
            key={item.id}
            onClick={() => onChangeView(item.id)}
            className={`flex flex-col items-center gap-1 p-2 w-16 transition-colors ${isActive ? 'text-indigo-600' : 'text-gray-300'}`}
          >
            <item.icon className={`w-6 h-6 ${isActive ? 'fill-current' : ''}`} strokeWidth={isActive ? 2.5 : 2} />
            <span className="text-[10px] font-bold">{item.label}</span>
          </button>
        );
      })}
    </nav>
  );
};

// --- Main App ---
export default function App() {
  const [view, setView] = useState('loading'); // 앱 시작 시 토큰 체크
  const [user, setUser] = useState({ nickname: '', age: '', gender: '' });
  const [tempToken, setTempToken] = useState(null); // 신규회원 임시 토큰
  useEffect(() => {
    const checkAuth = async () => {
      const token = getToken();
      if (!token) { setView('login'); return; }
      try {
        const userData = await api('/users/me');
        setUser({ nickname: userData.nickname, age: userData.age, gender: '' });
        fetchRecords();
        setView('home');
      } catch (e) {
        clearTokens();
        setView('login');
      }
    };
    checkAuth();
  }, []);

  const fetchRecords = async (year = new Date().getFullYear(), month = new Date().getMonth() + 1) => {
    try {
      const monthRecords = await api(`/records/monthly?year=${year}&month=${month}`);
      setRecords(monthRecords.map(r => ({
        ...r,
        emotionId: r.emotionType,
      })));
    } catch (e) {
      console.error('Failed to fetch records', e);
    }
  };

  // 구글 로그인 버튼 렌더링

  useEffect(() => {
    if (view === 'login' && window.google && googleBtnRef.current) {
      window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: handleGoogleResponse,
      });
      window.google.accounts.id.renderButton(googleBtnRef.current, {
        theme: 'outline',
        size: 'large',
        width: 300,
        text: 'continue_with',
        locale: 'ko',
      });
    }
  }, [view]);

  // Real Data State
  const [records, setRecords] = useState([]);

  // Interaction Stat
  const [toast, setToast] = useState({ show: false, message: '', recordId: null });
  // modal types: 'detail_write', 'solution_view', 'share_monthly', 'share_daily', 'delete_confirm'
  const [modal, setModal] = useState({ type: null, data: null });
  const [isAiLoading, setIsAiLoading] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);

  // Quick Record State
  const [activeQuickEmotion, setActiveQuickEmotion] = useState(null);

  // Helper to find currently selected emotion object
  const selectedEmotion = useMemo(() =>
    EMOTIONS.find(e => e.id === activeQuickEmotion),
    [activeQuickEmotion]
  );

  // Logic Functions

  const toggleQuickEmotion = (emotionId) => {
    if (activeQuickEmotion === emotionId) {
      setActiveQuickEmotion(null);
    } else {
      setActiveQuickEmotion(emotionId);
    }
  };

  const handleLevelSelect = async (emotion, step) => {
    const level = step * 20;
    try {
      const res = await api('/records', {
        method: 'POST',
        body: { emotionType: emotion.id, level, reason: "" }
      });
      // Response includes { id, message }
      const newRecord = {
        id: res.id,
        emotionId: emotion.id,
        timestamp: new Date().toISOString(),
        level: level,
        reason: "",
        solution: null
      };
      setRecords(prev => [...prev, newRecord]);
      setToast({ show: true, message: `${emotion.label} (${level}%) 기록 완료`, recordId: newRecord.id });
    } catch (err) {
      alert("저장에 실패했습니다.");
    }
    setActiveQuickEmotion(null);
  };

  const openDetailModal = (recordId) => {
    const record = records.find(r => r.id === recordId);
    if (record) {
      setModal({ type: 'detail_write', data: record });
      setToast({ ...toast, show: false });
    }
  };

  // --- Delete Logic ---
  const requestDelete = (recordId) => {
    setModal({ type: 'delete_confirm', data: recordId });
  };

  const confirmDelete = async () => {
    const recordId = modal.data;
    try {
      await api(`/records/${recordId}`, { method: 'DELETE' });
      setRecords(prev => prev.filter(r => r.id !== recordId));
      setModal({ type: null, data: null });
      setToast({ show: true, message: "기록이 삭제되었습니다.", recordId: null });
    } catch (err) {
      alert("삭제에 실패했습니다.");
    }
  };

  const cancelDelete = () => {
    const record = records.find(r => r.id === modal.data);
    if (record) {
      setModal({ type: 'detail_write', data: record });
    } else {
      setModal({ type: null, data: null });
    }
  };

  const handleSaveDetail = async (recordId, updates, requestAi) => {
    try {
      await api(`/records/${recordId}`, {
        method: 'PATCH',
        body: { emotionType: updates.emotionId, level: updates.level, reason: updates.reason }
      });

      let finalSolution = modal.data.solution;

      if (requestAi) {
        setIsAiLoading(true);
        const aiRes = await api('/solutions/generate', {
          method: 'POST',
          body: { recordId }
        });
        finalSolution = { content: aiRes.content, evaluation: 0, id: aiRes.solutionId };
        setIsAiLoading(false);
      }

      setRecords(prev => prev.map(r => r.id === recordId ? { ...r, ...updates, emotionId: updates.emotionId, solution: finalSolution } : r));
      const updatedRecord = { ...modal.data, ...updates, emotionId: updates.emotionId, solution: finalSolution };
      setModal(prev => ({ ...prev, data: updatedRecord }));

      if (!requestAi) {
        setToast({ show: true, message: "저장되었습니다.", recordId: null });
      }
    } catch (err) {
      setIsAiLoading(false);
      alert("오류가 발생했습니다.");
    }
  };

  const handleFeedback = async (recordId, score) => {
    const record = records.find(r => r.id === recordId);
    if (!record || !record.solution) return;
    try {
      const solutionIdToUse = record.solution.id || record.solution.solutionId;
      console.log('Sending feedback - recordId:', recordId, 'score:', score, 'solutionIdToUse:', solutionIdToUse, 'record:', record);

      if (!solutionIdToUse) {
        alert("솔루션 ID를 찾을 수 없어 백엔드로 평가를 전송할 수 없습니다. 다시 솔루션을 받아주세요.");
        return;
      }

      const response = await api(`/solutions/${solutionIdToUse}/feedback`, {
        method: 'POST',
        body: { score }
      });
      console.log('Feedback response:', response);

      setRecords(prev => prev.map(r => r.id === recordId ? { ...r, solution: { ...r.solution, evaluation: score } } : r));
      if (modal.type === 'detail_write' && modal.data.id === recordId) {
        setModal(prev => ({ ...prev, data: { ...prev.data, solution: { ...prev.data.solution, evaluation: score } } }));
      }
      setToast({ show: true, message: "평가되었습니다.", recordId: null });
    } catch (e) {
      console.error('Feedback error:', e);
      alert(`평가 전송 실패: ${e.message}`);
    }
  };
  // 로그아웃
  const handleLogout = async () => {
    try {
      await api('/auth/logout', { method: 'POST' });
    } catch (e) {
      console.warn('Logout API failed:', e);
    } finally {
      clearTokens();
      setProfileOpen(false);
      setRecords([]);
      setView('login');
    }
  };

  // 회원 탈퇴
  const handleDeleteAccount = async () => {
    try {
      await api('/users/me', { method: 'DELETE' });
    } catch (e) {
      console.warn('Delete account API failed:', e);
    } finally {
      clearTokens();
      setProfileOpen(false);
      setRecords([]);
      setModal({ type: null, data: null });
      setView('login');
    }
  };

  // 구글 로그인 Ref & 응답 처리
  const googleBtnRef = useRef(null);

  const handleGoogleResponse = async (response) => {
    try {
      const data = await api('/auth/login/google', {
        method: 'POST',
        body: { token: response.credential },
      });
      if (data.isNewUser) {
        setTempToken(data.accessToken);
        setView('signup');
      } else {
        setTokens(data.accessToken, data.refreshToken);
        const userData = await api('/users/me');
        setUser({ nickname: userData.nickname, age: userData.age, gender: '' });
        fetchRecords();
        setView('home');
      }
    } catch (err) {
      alert('로그인에 실패했습니다. 다시 시도해주세요.');
      console.error(err);
    }
  };
  // --- Views ---
  //login view
  const LoginView = () => (
    <div className="h-full flex flex-col items-end justify-end p-6 animate-in fade-in relative overflow-hidden bg-blue-900">
      <img src={campusBg} alt="" className="absolute inset-0 w-full h-full object-cover object-center opacity-40" />
      <div className="absolute inset-0 bg-gradient-to-t from-blue-900 via-blue-900/70 to-transparent"></div>
      <div className="w-full text-center relative z-10 mb-12">
        <h1 className="text-4xl font-extrabold text-white mb-1">Emotion Log</h1>
        <p className="text-blue-200 text-sm mb-8">감정 기록부터 AI 솔루션까지</p>
        <div className="flex justify-center">
          <div ref={googleBtnRef}></div>
        </div>
      </div>
    </div>
  );
  // 2. Signup View
  const SignupView = () => (
    <div className="h-full p-6 bg-white flex flex-col animate-in slide-in-from-right">
      <div className="flex items-center mb-8 mt-2">
        <button onClick={() => setView('login')}><ChevronLeft className="w-6 h-6 text-gray-600" /></button>
        <h2 className="text-2xl font-bold ml-2">회원가입</h2>
      </div>
      <div className="space-y-6 flex-1">
        <div>
          <label className="block text-sm font-bold text-gray-700 mb-2">닉네임</label>
          <input
            className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none bg-gray-50 transition-colors focus:bg-white"
            placeholder="사용하실 닉네임을 입력하세요"
            value={user.nickname}
            onChange={(e) => setUser({ ...user, nickname: e.target.value })}
          />
        </div>
        <div>
          <label className="block text-sm font-bold text-gray-700 mb-2">나이</label>
          <input
            type="number"
            className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none bg-gray-50 transition-colors focus:bg-white"
            placeholder="24"
            value={user.age}
            onChange={(e) => setUser({ ...user, age: e.target.value })}
          />
        </div>
        <div>
          <label className="block text-sm font-bold text-gray-700 mb-2">성별</label>
          <div className="flex gap-3">
            {['male', 'female'].map((type) => (
              <button
                key={type}
                onClick={() => setUser({ ...user, gender: type })}
                className={`flex-1 py-3 rounded-xl font-bold border transition-all duration-200
                  ${user.gender === type
                    ? 'border-indigo-600 bg-indigo-50 text-indigo-600 shadow-sm'
                    : 'border-gray-200 bg-gray-50 text-gray-400 hover:bg-gray-100'
                  }
                `}
              >
                {type === 'male' ? '남성' : '여성'}
              </button>
            ))}
          </div>
        </div>
      </div>
      <Button onClick={async () => {
        try {
          const data = await fetch(`${API_BASE}/auth/signup`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${tempToken}`,
            },
            body: JSON.stringify({
              nickname: user.nickname,
              age: Number(user.age),
              gender: user.gender,
            }),
          }).then(r => r.json());

          setTokens(data.accessToken, data.refreshToken);
          setView('home');
        } catch (err) {
          alert('회원가입에 실패했습니다.');
          console.error(err);
        }
      }} disabled={!user.nickname || !user.age || !user.gender}>
        가입 완료
      </Button>
    </div>
  );

  // 3. Home View
  const HomeView = () => (
    <div className="space-y-8 animate-in fade-in duration-500 pt-4">
      <section>
        <h2 className="text-2xl font-bold text-gray-800 leading-tight">
          반가워요, {user.nickname || '사용자'}님<br />
          <span className="text-indigo-600">오늘의 기분</span>은 어떤가요?
        </h2>
      </section>

      {/* Emotion Grid */}
      <section className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          {EMOTIONS.map(emo => {
            const isActive = activeQuickEmotion === emo.id;
            return (
              <button
                key={emo.id}
                onClick={() => toggleQuickEmotion(emo.id)}
                className={`group p-5 rounded-2xl border bg-gray-50 hover:bg-white hover:shadow-lg transition-all duration-200 text-left flex flex-col gap-3 active:scale-95 ring-1
                  ${isActive ? `bg-white shadow-lg ring-2 ring-indigo-500 border-indigo-200 scale-105 z-10` : `border-transparent ring-gray-100 hover:ring-indigo-100`}
                `}
              >
                <div className="flex flex-col items-center w-full">
                  <img src={emo.img} alt={emo.label} className="w-24 h-24 object-contain" />
                  {isActive && <CheckCircle className="w-5 h-5 text-indigo-500" />}
                </div>
                <span className={`font-bold text-lg ${isActive ? 'text-indigo-900' : 'text-gray-700'}`}>{emo.label}</span>
              </button>
            )
          })}
        </div>

        {/* Level Selector - Fixed Below Grid */}
        {selectedEmotion && (
          <div className="bg-white p-5 rounded-2xl border border-indigo-100 shadow-xl animate-in slide-in-from-top-4 fade-in duration-300">
            <div className="text-center mb-4">
              <span className={`inline-block px-3 py-1 rounded-full text-xs font-bold mb-2 ${selectedEmotion.bgSoft} ${selectedEmotion.text}`}>
                {selectedEmotion.label}
              </span>
              <p className="text-sm font-bold text-gray-800">어느 정도로 느끼시나요?</p>
            </div>

            <div className="flex justify-between gap-3">
              {[1, 2, 3, 4, 5].map((step) => (
                <button
                  key={step}
                  onClick={() => handleLevelSelect(selectedEmotion, step)}
                  className={`flex-1 aspect-square rounded-xl flex items-center justify-center font-bold text-lg transition-all
                      ${selectedEmotion.bgSoft} ${selectedEmotion.text} 
                      hover:scale-110 hover:shadow-md border border-transparent hover:border-${selectedEmotion.text.split('-')[1]}-200
                  `}
                >
                  {step}
                </button>
              ))}
            </div>
            <div className="flex justify-between text-xs text-gray-400 mt-3 px-1 font-medium">
              <span>약함 (20%)</span>
              <span>강함 (100%)</span>
            </div>
          </div>
        )}
      </section>

      <section>
        <div className="flex justify-between items-center mb-4">
          <h3 className="font-bold text-gray-800">최근 기록</h3>
        </div>
        <div className="space-y-3">
          {records.length === 0 ? (
            <div className="text-center py-8 bg-gray-50 rounded-xl text-gray-400 text-sm">기록이 없어요.</div>
          ) : (
            [...records].sort((a, b) => b.timestamp - a.timestamp).slice(0, 3).map(rec => {
              const emo = EMOTIONS.find(e => e.id === rec.emotionId);
              return (
                <div key={rec.id} onClick={() => openDetailModal(rec.id)} className="flex items-center gap-4 p-4 bg-white border border-gray-100 rounded-xl shadow-sm active:bg-gray-50 transition-colors">
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center ${emo?.bgSoft}`}>
                    {emo && <emo.icon className={`w-5 h-5 ${emo.text}`} />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between">
                      <span className="font-bold text-gray-700">{emo?.label} <span className="text-indigo-600 text-xs font-normal">({rec.level}%)</span></span>
                      <span className="text-xs text-gray-400">
                        {new Date(rec.timestamp).toLocaleDateString()} {new Date(rec.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    <p className="text-xs text-gray-500 truncate mt-1">{rec.reason || "상세 기록 없음"}</p>
                  </div>
                </div>
              )
            })
          )}
        </div>
      </section>
    </div>
  );

  // 4. Calendar Tab (With Share Logic)
  const CalendarTab = ({ onShareMonthly, onShareDaily }) => {
    const [currentDate, setCurrentDate] = useState(new Date());
    const [selectedDateStr, setSelectedDateStr] = useState(null);

    const getDaysInMonth = (date) => {
      const year = date.getFullYear();
      const month = date.getMonth();
      const days = new Date(year, month + 1, 0).getDate();
      const firstDay = new Date(year, month, 1).getDay();
      return { days, firstDay, year, month };
    };

    const { days, firstDay, year, month } = getDaysInMonth(currentDate);

    // Daily View
    if (selectedDateStr) {
      const dailyRecords = records.filter(r => formatDate(r.timestamp) === selectedDateStr);
      return (
        <div className="flex flex-col h-full animate-in slide-in-from-right">
          <header className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-2">
              <button onClick={() => setSelectedDateStr(null)} className="p-2 hover:bg-gray-100 rounded-full"><ChevronLeft className="w-6 h-6" /></button>
              <h2 className="text-xl font-bold">{selectedDateStr}</h2>
            </div>
            {dailyRecords.length > 0 && (
              <button
                onClick={() => onShareDaily(selectedDateStr, dailyRecords)}
                className="flex items-center gap-1 text-sm font-bold text-indigo-600 bg-indigo-50 px-3 py-1.5 rounded-lg hover:bg-indigo-100 transition-colors"
              >
                <Share2 className="w-4 h-4" /> 일간 통계 보기
              </button>
            )}
          </header>

          {/* Graph Area */}
          <div className="mb-8 relative h-64 bg-gray-50 rounded-3xl border border-gray-100 p-4">
            {/* Grid Lines */}
            <div className="absolute inset-x-6 top-8 bottom-8 flex flex-col justify-between text-gray-200 pointer-events-none">
              <div className="border-t border-dashed border-gray-200 w-full h-px"></div>
              <div className="border-t border-dashed border-gray-200 w-full h-px"></div>
              <div className="border-t border-dashed border-gray-200 w-full h-px"></div>
            </div>

            {/* Data Points */}
            {dailyRecords.length > 0 ? dailyRecords.map(record => {
              const date = new Date(record.timestamp);
              const totalMinutes = date.getHours() * 60 + date.getMinutes();
              // 하루 1440분 기준 위치 계산
              const left = (totalMinutes / 1440) * 100;
              // 감정 강도 (0-100) 기준 위치 계산
              const bottom = record.level;
              const emo = EMOTIONS.find(e => e.id === record.emotionId);

              return (
                <button
                  key={record.id}
                  onClick={() => openDetailModal(record.id)}
                  style={{ left: `${left}%`, bottom: `${bottom}%` }}
                  className={`absolute w-8 h-8 -ml-4 -mb-4 rounded-full border-2 border-white shadow-md flex items-center justify-center transition-transform hover:scale-125 active:scale-95 z-10 ${emo.color}`}
                >
                  <emo.icon className="w-4 h-4 text-white" />
                </button>
              );
            }) : (
              <div className="absolute inset-0 flex items-center justify-center text-gray-400 text-sm">
                기록된 감정이 없습니다.
              </div>
            )}

            {/* X Axis Labels */}
            <div className="absolute bottom-2 left-4 right-4 flex justify-between text-[10px] text-gray-400">
              <span>00:00</span><span>12:00</span><span>24:00</span>
            </div>
          </div>

          {/* List */}
          <div className="space-y-3">
            {dailyRecords.map(rec => {
              const emo = EMOTIONS.find(e => e.id === rec.emotionId);
              return (
                <div key={rec.id} onClick={() => openDetailModal(rec.id)} className="flex items-start gap-4 p-4 bg-white border border-gray-100 rounded-2xl shadow-sm hover:bg-gray-50 transition-colors cursor-pointer">
                  <div className={`p-2 rounded-xl ${emo.bgSoft}`}><emo.icon className={`w-5 h-5 ${emo.text}`} /></div>
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-center mb-1">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-gray-800">{emo.label} <span className="text-indigo-600 text-xs ml-1 font-normal">({rec.level}%)</span></span>
                        {/* Solution Indicator Icon */}
                        {rec.solution && <Zap className="w-3 h-3 text-yellow-500 fill-yellow-500" />}
                      </div>
                      <span className="text-xs text-gray-400">{new Date(rec.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    </div>
                    <p className="text-sm text-gray-600 truncate">{rec.reason || "상세 내용 없음"}</p>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      );
    }

    // Monthly View
    return (
      <div className="h-full flex flex-col animate-in fade-in">
        <header className="flex justify-between items-center mb-6">
          <div className="flex items-center gap-1">
            <button onClick={() => setCurrentDate(new Date(year, month - 1, 1))} className="p-1 hover:bg-gray-100 rounded-full"><ChevronLeft className="w-5 h-5" /></button>
            <h2 className="text-xl font-bold">{year}년 {month + 1}월</h2>
            <button onClick={() => setCurrentDate(new Date(year, month + 1, 1))} className="p-1 hover:bg-gray-100 rounded-full"><ChevronRight className="w-5 h-5" /></button>
          </div>
          <button
            onClick={() => onShareMonthly(year, month + 1, records)}
            className="flex items-center gap-1 text-sm font-bold text-indigo-600 bg-indigo-50 px-3 py-1.5 rounded-lg hover:bg-indigo-100 transition-colors"
          >
            <TrendingUp className="w-4 h-4" /> 이번 달 통계 보기
          </button>
        </header>

        <div className="grid grid-cols-7 gap-2 mb-2 text-center text-xs text-gray-400 font-medium">
          <span className="text-red-400">일</span><span>월</span><span>화</span><span>수</span><span>목</span><span>금</span><span className="text-blue-400">토</span>
        </div>

        <div className="grid grid-cols-7 gap-2 auto-rows-fr">
          {Array.from({ length: firstDay }, (_, i) => <div key={`empty-${i}`} />)}
          {Array.from({ length: days }, (_, i) => {
            const day = i + 1;
            const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
            const dayRecords = records.filter(r => formatDate(r.timestamp) === dateStr);
            const dominantRecord = dayRecords.length > 0 ? dayRecords.reduce((prev, curr) => prev.level > curr.level ? prev : curr) : null;
            const emo = dominantRecord ? EMOTIONS.find(e => e.id === dominantRecord.emotionId) : null;

            return (
              <button
                key={day}
                onClick={() => setSelectedDateStr(dateStr)}
                className={`aspect-square rounded-xl border flex flex-col items-center justify-center text-sm font-medium transition-all relative
                                ${emo ? `${emo.color} text-white border-transparent shadow-sm` : 'bg-white border-gray-100 text-gray-400 hover:bg-gray-50'}`}
              >
                {day}
                {dayRecords.length > 1 && <div className="absolute bottom-1.5 w-1 h-1 bg-white rounded-full opacity-70"></div>}
              </button>
            );
          })}
        </div>
      </div>
    );
  };

  return (
    <div className="flex justify-center min-h-screen bg-gray-100 font-sans">
      <div className="w-full max-w-md bg-white shadow-xl min-h-screen relative flex flex-col overflow-hidden">

        {/* Header */}
        {!['login', 'signup'].includes(view) && (
          <header className="px-6 py-5 flex justify-between items-center bg-white z-10 sticky top-0">
            <h1 className="text-xl font-extrabold text-gray-900 tracking-tight">Emotion Log</h1>
            <div className="relative">
              <button
                onClick={() => setProfileOpen(prev => !prev)}
                className="w-8 h-8 bg-indigo-100 rounded-full flex items-center justify-center hover:bg-indigo-200 transition-colors"
              >
                <User className="w-5 h-5 text-indigo-600" />
              </button>
              {profileOpen && (
                <>
                  {/* 바깥 클릭 시 닫기 */}
                  <div className="fixed inset-0 z-40" onClick={() => setProfileOpen(false)} />
                  <div className="absolute right-0 mt-2 w-52 bg-white rounded-2xl shadow-xl border border-gray-100 z-50 overflow-hidden animate-in fade-in slide-in-from-top-2 duration-200">
                    <div className="px-4 py-3 border-b border-gray-100 bg-gray-50">
                      <p className="text-xs text-gray-400 font-medium">로그인된 계정</p>
                      <p className="text-sm font-bold text-gray-800 mt-0.5 truncate">{user.nickname || '사용자'}</p>
                    </div>
                    <button
                      onClick={handleLogout}
                      className="w-full flex items-center gap-2 px-4 py-3 text-sm text-red-500 font-semibold hover:bg-red-50 transition-colors"
                    >
                      <LogOut className="w-4 h-4" />
                      로그아웃
                    </button>
                    <div className="border-t border-gray-100" />
                    <button
                      onClick={() => { setProfileOpen(false); setModal({ type: 'delete_account', data: null }); }}
                      className="w-full flex items-center gap-2 px-4 py-3 text-xs text-gray-400 font-medium hover:bg-gray-50 transition-colors"
                    >
                      회원 탈퇴
                    </button>
                  </div>
                </>
              )}
            </div>
          </header>
        )}

        {/* Content */}
        <main className={`flex-1 overflow-y-auto px-6 scrollbar-hide ${!['login', 'signup'].includes(view) ? 'pb-24' : ''}`}>
          {view === 'loading' && (
            <div className="h-full flex items-center justify-center">
              <Loader2 className="w-8 h-8 animate-spin text-indigo-600" />
            </div>
          )}
          {view === 'login' && LoginView()}
          {view === 'signup' && SignupView()}
          {view === 'home' && HomeView()}
          {view === 'calendar' && (
            <CalendarTab
              onShareMonthly={(y, m, recs) => setModal({ type: 'share_monthly', data: { year: y, month: m, records: recs } })}
              onShareDaily={(date, recs) => setModal({ type: 'share_daily', data: { date, records: recs } })}
            />
          )}
        </main>

        {/* Global Components */}
        {!['login', 'signup'].includes(view) && <BottomNav currentView={view} onChangeView={setView} />}
        <Toast
          show={toast.show}
          message={toast.message}
          onClose={() => setToast({ ...toast, show: false })}
          onAction={toast.recordId ? () => openDetailModal(toast.recordId) : null}
          actionLabel="이유 적기"
        />

        {/* Modals */}
        {modal.type === 'detail_write' && (
          <DetailModal
            record={modal.data}
            onClose={() => setModal({ type: null, data: null })}
            onSave={handleSaveDetail}
            onDelete={requestDelete}
            onFeedback={handleFeedback}
            isLoading={isAiLoading}
          />
        )}
        {modal.type === 'solution_view' && <SolutionModal data={modal.data} onClose={() => setModal({ type: null, data: null })} />}

        {/* New Share Modals (Recap Style) */}
        {modal.type === 'share_daily' && <ShareDailyModal data={modal.data} onClose={() => setModal({ type: null, data: null })} />}
        {modal.type === 'share_monthly' && <ShareMonthlyModal data={modal.data} onClose={() => setModal({ type: null, data: null })} />}

        {/* Delete Confirmation Modal */}
        {modal.type === 'delete_confirm' && (
          <DeleteConfirmModal
            onConfirm={confirmDelete}
            onCancel={cancelDelete}
          />
        )}

        {/* Delete Account Confirmation Modal */}
        {modal.type === 'delete_account' && (
          <div className="absolute inset-0 bg-black/50 z-50 flex items-center justify-center p-6">
            <div className="bg-white rounded-2xl p-6 shadow-2xl w-full max-w-sm">
              <h3 className="text-lg font-bold text-gray-900 mb-2">회원 탈퇴</h3>
              <p className="text-sm text-gray-500 mb-6 leading-relaxed">
                정말로 탈퇴하시겠습니까?<br />
                모든 감정 기록과 AI 솔루션이 영구 삭제되며 복구할 수 없습니다.
              </p>
              <div className="flex gap-3">
                <button
                  onClick={() => setModal({ type: null, data: null })}
                  className="flex-1 py-3 rounded-xl border border-gray-200 text-gray-600 font-semibold text-sm hover:bg-gray-50 transition-colors"
                >
                  취소
                </button>
                <button
                  onClick={handleDeleteAccount}
                  className="flex-1 py-3 rounded-xl bg-red-500 text-white font-semibold text-sm hover:bg-red-600 transition-colors"
                >
                  탈퇴하기
                </button>
              </div>
            </div>
          </div>
        )}
        {modal.type === 'delete_confirm' && (
          <DeleteConfirmModal
            onConfirm={confirmDelete}
            onCancel={cancelDelete}
          />
        )}
      </div>
    </div>
  );
}

// --- Modals ---

const DetailModal = ({ record, onClose, onSave, onDelete, onFeedback, isLoading }) => {
  // Use state for editing emotion type
  const [selectedEmotionId, setSelectedEmotionId] = useState(record.emotionId);
  const [level, setLevel] = useState(record.level);
  const [reason, setReason] = useState(record.reason || '');

  // Derived states
  const currentEmotion = EMOTIONS.find(e => e.id === selectedEmotionId);
  const currentStep = level / 20;
  const hasSolution = !!record.solution;

  // Handlers for separated buttons
  const handleJustSave = () => {
    onSave(record.id, { emotionId: selectedEmotionId, level, reason }, false); // false = AI 요청 안함
  };

  const handleRequestAI = () => {
    onSave(record.id, { emotionId: selectedEmotionId, level, reason }, true); // true = AI 요청 함
  };

  return (
    <div className="absolute inset-0 bg-white z-50 flex flex-col animate-in slide-in-from-bottom duration-300">
      <div className="px-6 py-4 flex items-center justify-between border-b border-gray-100">
        <button onClick={onClose}><ChevronLeft className="w-6 h-6 text-gray-600" /></button>
        <span className="font-bold text-gray-800">상세 기록</span>
        {/* Delete Button */}
        <button onClick={() => onDelete(record.id)} className="p-2 text-gray-400 hover:text-red-500 transition-colors">
          <Trash2 className="w-5 h-5" />
        </button>
      </div>
      <div className="flex-1 overflow-y-auto p-6 space-y-8 scrollbar-hide">

        {/* 1. Emotion Selector */}
        <div className="flex flex-col items-center">
          {/* Selected Big Icon */}
          <img src={currentEmotion.img} alt={currentEmotion.label} className="w-32 h-32 object-contain mb-4" />
          <h2 className="text-2xl font-bold text-gray-800 mb-6">{currentEmotion.label}</h2>

          {/* Quick Change List (only if editable, logic simplified) */}
          <div className="flex gap-3 bg-gray-50 p-2 rounded-2xl overflow-x-auto w-full justify-center">
            {EMOTIONS.map(emo => (
              <button
                key={emo.id}
                onClick={() => setSelectedEmotionId(emo.id)}
                className={`p-3 rounded-xl transition-all ${selectedEmotionId === emo.id ? 'bg-white shadow-sm ring-2 ring-indigo-500 scale-110' : 'hover:bg-white/50'}`}
              >
                <img src={emo.img} alt={emo.label} className="w-10 h-10 object-contain" />
              </button>
            ))}
          </div>
        </div>

        {/* 2. Level Selector (5 Steps) */}
        <div>
          <div className="flex justify-between mb-3">
            <span className="font-bold text-gray-700">감정의 강도</span>
            <span className="text-indigo-600 font-bold">{level}%</span>
          </div>
          <div className="flex justify-between gap-2">
            {[1, 2, 3, 4, 5].map((step) => (
              <button
                key={step}
                onClick={() => setLevel(step * 20)}
                className={`flex-1 aspect-square rounded-xl flex items-center justify-center font-bold text-lg transition-all border
                      ${currentStep === step
                    ? `${currentEmotion.bgSoft} ${currentEmotion.text} border-${currentEmotion.text.split('-')[1]}-200 ring-2 ring-${currentEmotion.text.split('-')[1]}-200`
                    : 'bg-white border-gray-100 text-gray-400 hover:bg-gray-50'
                  }
                  `}
              >
                {step}
              </button>
            ))}
          </div>
          <div className="flex justify-between text-xs text-gray-400 mt-2 px-1">
            <span>약함</span>
            <span>강함</span>
          </div>
        </div>

        {/* 3. Text Area */}
        <textarea
          className="w-full h-40 p-4 bg-gray-50 border border-gray-100 rounded-xl focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none transition-all resize-none text-sm leading-relaxed"
          placeholder="상황이나 생각을 자유롭게 적어주세요."
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />

        {/* 4. AI Solution Section (Fixed Area) */}
        <div className="relative overflow-hidden rounded-2xl border border-indigo-100 p-6 shadow-sm transition-all duration-300 bg-gradient-to-br from-indigo-50 to-purple-50">
          {/* Background Decor */}
          <div className="absolute top-0 right-0 -mt-4 -mr-4 w-24 h-24 bg-white opacity-50 rounded-full blur-2xl"></div>

          <div className="relative z-10">
            <div className="flex items-center gap-2 mb-4">
              <div className="p-2 bg-white rounded-lg shadow-sm">
                <Zap className={`w-5 h-5 ${hasSolution ? 'text-indigo-500 fill-indigo-500' : 'text-gray-400'}`} />
              </div>
              <h3 className={`font-bold ${hasSolution ? 'text-indigo-900' : 'text-gray-500'}`}>AI 마음 처방전</h3>
            </div>

            {hasSolution ? (
              <>
                <p className="text-gray-700 text-sm leading-relaxed mb-6 bg-white/60 p-4 rounded-xl backdrop-blur-sm border border-white/50 animate-in fade-in">
                  {record.solution.content}
                </p>
                {/* Rating Feedback */}
                <div className="border-t border-indigo-100 pt-4">
                  <p className="text-xs font-bold text-gray-500 mb-2 text-center">이 조언이 도움이 되었나요?</p>
                  <div className="flex justify-center gap-2">
                    {[1, 2, 3, 4, 5].map((star) => (
                      <button
                        key={star}
                        onClick={() => onFeedback(record.id, star)}
                        className="focus:outline-none transition-transform hover:scale-110 active:scale-95"
                      >
                        <Star
                          className={`w-8 h-8 ${star <= record.solution.evaluation
                            ? 'fill-yellow-400 text-yellow-400'
                            : 'text-gray-300'}`}
                        />
                      </button>
                    ))}
                  </div>
                </div>
              </>
            ) : (
              <div className="text-center py-6 text-gray-400 text-sm">
                <p>아직 받은 처방이 없습니다.</p>
                <p className="text-xs mt-1">내용을 입력하고 왼쪽 버튼을 눌러보세요.</p>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Footer Buttons (Separated & Reordered) */}
      <div className="p-6 border-t border-gray-100 bg-white pb-safe flex gap-3">
        {/* Left: AI Request (Secondary Action) */}
        <Button
          onClick={handleRequestAI}
          disabled={reason.trim().length === 0 || isLoading}
          className={`flex-1 ${isLoading ? "bg-indigo-50 text-indigo-300" : "bg-indigo-50 text-indigo-600 hover:bg-indigo-100 border border-indigo-100 shadow-none"}`}
        >
          {isLoading ? (
            <div className="flex items-center justify-center gap-2">
              <Loader2 className="w-5 h-5 animate-spin" />
            </div>
          ) : (
            <div className="flex items-center justify-center gap-2">
              <Zap className="w-4 h-4" /> {hasSolution ? "다시 받기" : "AI 솔루션"}
            </div>
          )}
        </Button>

        {/* Right: Save (Primary Action) */}
        <Button
          variant="primary"
          onClick={handleJustSave}
          disabled={isLoading}
          className="flex-[2]"
        >
          기록 저장
        </Button>
      </div>
    </div>
  );
};

const DeleteConfirmModal = ({ onConfirm, onCancel }) => {
  return (
    <div className="absolute inset-0 bg-black bg-opacity-50 z-[60] flex items-center justify-center p-6 animate-in fade-in">
      <div className="bg-white w-full max-w-xs rounded-2xl p-6 shadow-2xl text-center">
        <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <Trash2 className="w-6 h-6 text-red-500" />
        </div>
        <h3 className="text-lg font-bold text-gray-900 mb-2">기록 삭제</h3>
        <p className="text-gray-500 text-sm mb-6">정말 이 기록을 삭제하시겠습니까?<br />삭제된 내용은 복구할 수 없습니다.</p>
        <div className="flex gap-3">
          <Button variant="secondary" onClick={onCancel}>취소</Button>
          <Button variant="danger" onClick={onConfirm}>삭제</Button>
        </div>
      </div>
    </div>
  );
};

const SolutionModal = ({ data, onClose }) => {
  return (
    <div className="absolute inset-0 bg-gray-50 z-50 flex items-center justify-center p-6 animate-in fade-in">
      <div className="bg-white w-full rounded-2xl p-6 shadow-xl relative">
        <button onClick={onClose} className="absolute top-4 right-4"><X className="w-5 h-5 text-gray-400" /></button>
        <h2 className="text-xl font-bold text-center mb-4">AI 솔루션</h2>
        <p className="text-gray-600">{data.solution.content}</p>
        <div className="mt-6 flex justify-center"><Button onClick={onClose}>확인</Button></div>
      </div>
    </div>
  )
}


// --- 🌟 NEW: Share Modals (Recap Style) ---

// 1. Daily Share Card
const ShareDailyModal = ({ data, onClose }) => {
  const cardRef = useRef(null);
  const [isSaving, setIsSaving] = useState(false);
  const record = data.records.reduce((prev, curr) => prev.level > curr.level ? prev : curr);
  const emotion = EMOTIONS.find(e => e.id === record.emotionId);

  const handleSaveAndShare = useCallback(async () => {
    if (!cardRef.current || isSaving) return;
    setIsSaving(true);
    try {
      const canvas = await html2canvas(cardRef.current, {
        scale: 2,
        useCORS: true,
        backgroundColor: null,
      });

      // 공유 가능 여부 확인 (모바일 등)
      if (navigator.share && navigator.canShare) {
        canvas.toBlob(async (blob) => {
          const file = new File([blob], `emotion-daily-${data.date}.png`, { type: 'image/png' });
          if (navigator.canShare({ files: [file] })) {
            await navigator.share({ files: [file], title: '오늘의 감정 기록' });
          } else {
            downloadCanvas(canvas);
          }
        });
      } else {
        downloadCanvas(canvas);
      }
    } catch (err) {
      console.error('캡처 실패:', err);
      alert('이미지 저장에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsSaving(false);
    }
  }, [data.date, isSaving]);

  const downloadCanvas = (canvas) => {
    const link = document.createElement('a');
    link.download = `emotion-daily-${data.date}.png`;
    link.href = canvas.toDataURL('image/png');
    link.click();
  };

  return (
    <div className="absolute inset-0 bg-black bg-opacity-80 z-50 flex flex-col items-center justify-center p-6 animate-in fade-in backdrop-blur-sm">
      <div className="w-full max-w-sm relative">
        <button onClick={onClose} className="absolute -top-12 right-0 p-2 text-white bg-white/20 rounded-full"><X className="w-6 h-6" /></button>

        {/* The Card (9:16 Ratio approx) */}
        <div ref={cardRef} className={`w-full aspect-[9/16] bg-gradient-to-br ${emotion.gradient} rounded-[2rem] shadow-2xl p-8 flex flex-col justify-between relative overflow-hidden border-4 border-white/30`}>

          {/* Decor */}
          <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full blur-3xl -mr-20 -mt-20"></div>
          <div className="absolute bottom-0 left-0 w-64 h-64 bg-black/5 rounded-full blur-3xl -ml-20 -mb-20"></div>

          {/* Header */}
          <div className="relative z-10">
            <p className="text-gray-500 font-bold opacity-60 text-sm tracking-widest uppercase">Daily Mood</p>
            <h3 className="text-3xl font-extrabold text-gray-800 mt-1">{data.date}</h3>
          </div>

          {/* Main Visual */}
          <div className="relative z-10 flex flex-col items-center">
            <div className={`w-32 h-32 rounded-full ${emotion.bgSoft} flex items-center justify-center shadow-2xl mb-6 ring-8 ring-white/40`}>
              <emotion.icon className={`w-16 h-16 ${emotion.text}`} />
            </div>
            <h2 className="text-2xl font-bold text-gray-800 mb-2">오늘 나는 <span className={emotion.text}>"{emotion.label}"</span></h2>
            <div className="flex items-center gap-2 bg-white/50 px-4 py-1.5 rounded-full">
              <Activity className="w-4 h-4 text-gray-500" />
              <span className="text-sm font-bold text-gray-600">강도 {record.level}%</span>
            </div>
          </div>

          {/* Footer / Solution Quote */}
          <div className="relative z-10 bg-white/60 p-5 rounded-2xl backdrop-blur-md">
            <Quote className="w-6 h-6 text-gray-400 mb-2 fill-gray-400 opacity-50" />
            <p className="text-gray-700 font-medium text-sm leading-relaxed line-clamp-3">
              {record.solution ? record.solution.content : record.reason || "기록된 내용이 없습니다."}
            </p>
          </div>

          {/* Brand */}
          <div className="absolute bottom-4 left-0 w-full text-center">
            <span className="text-[10px] font-bold text-gray-400 tracking-widest">EMOTION LOG</span>
          </div>
        </div>

        <div className="mt-6">
          <Button variant="primary" icon={isSaving ? Loader2 : Download} onClick={handleSaveAndShare} disabled={isSaving} className="bg-white text-indigo-600 hover:bg-gray-50 shadow-none">
            {isSaving ? '저장 중...' : '이미지 저장 및 공유'}
          </Button>
        </div>
      </div>
    </div>
  );
};
// 2. Monthly Share Card (Recap Style)
const ShareMonthlyModal = ({ data, onClose }) => {
  const cardRef = useRef(null);
  const [isSaving, setIsSaving] = useState(false);
  const { year, month, records } = data;

  const monthlyRecords = records.filter(r => {
    const d = new Date(r.timestamp);
    return d.getFullYear() === year && d.getMonth() + 1 === month;
  });

  const emotionCounts = EMOTIONS.map(emo => ({
    ...emo,
    count: monthlyRecords.filter(r => r.emotionId === emo.id).length
  })).sort((a, b) => b.count - a.count);

  const topEmotion = emotionCounts[0];
  const total = monthlyRecords.length || 1;
  const TopIcon = topEmotion.icon;

  const handleSaveAndShare = useCallback(async () => {
    if (!cardRef.current || isSaving) return;
    setIsSaving(true);
    try {
      const canvas = await html2canvas(cardRef.current, {
        scale: 2,
        useCORS: true,
        backgroundColor: null,
      });

      if (navigator.share && navigator.canShare) {
        canvas.toBlob(async (blob) => {
          const file = new File([blob], `emotion-monthly-${year}-${month}.png`, { type: 'image/png' });
          if (navigator.canShare({ files: [file] })) {
            await navigator.share({ files: [file], title: `${month}월의 감정 리캡` });
          } else {
            downloadCanvas(canvas);
          }
        });
      } else {
        downloadCanvas(canvas);
      }
    } catch (err) {
      console.error('캡처 실패:', err);
      alert('이미지 저장에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setIsSaving(false);
    }
  }, [year, month, isSaving]);

  const downloadCanvas = (canvas) => {
    const link = document.createElement('a');
    link.download = `emotion-monthly-${year}-${month}.png`;
    link.href = canvas.toDataURL('image/png');
    link.click();
  };

  return (
    <div className="absolute inset-0 bg-black bg-opacity-80 z-50 flex flex-col items-center justify-center p-6 animate-in fade-in backdrop-blur-sm">
      <div className="w-full max-w-sm relative">
        <button onClick={onClose} className="absolute -top-12 right-0 p-2 text-white bg-white/20 rounded-full"><X className="w-6 h-6" /></button>

        <div ref={cardRef} className="w-full aspect-[9/16] bg-gray-900 rounded-[2rem] shadow-2xl p-8 flex flex-col relative overflow-hidden border-4 border-gray-700 text-white">

          <div className="absolute top-0 right-0 w-80 h-80 bg-indigo-600/20 rounded-full blur-3xl -mr-20 -mt-20"></div>
          <div className="absolute bottom-0 left-0 w-64 h-64 bg-purple-600/20 rounded-full blur-3xl -ml-20 -mb-20"></div>

          <div className="relative z-10 mb-6">
            <h3 className="text-3xl font-extrabold text-white">{month}월의<br /><span className="text-indigo-400">감정 리캡</span></h3>
            <p className="text-gray-400 text-xs mt-2 uppercase tracking-widest">{year} EMOTION RECAP</p>
          </div>

          <div className="relative z-10 flex items-center gap-4 mb-8">
            <div className={`w-20 h-20 rounded-2xl ${topEmotion.color} flex items-center justify-center text-white shadow-lg shadow-indigo-500/30`}>
              <TopIcon className="w-10 h-10" />
            </div>
            <div>
              <p className="text-gray-300 text-sm">가장 많이 느낀 감정</p>
              <p className="text-2xl font-bold text-white">{topEmotion.label}</p>
              <p className="text-xs text-indigo-300 font-bold mt-1">총 {monthlyRecords.length}번 중 {topEmotion.count}번</p>
            </div>
          </div>

          <div className="relative z-10 flex-1">
            <h4 className="text-sm font-bold text-gray-400 mb-3 flex items-center gap-2">
              <BarChart2 className="w-4 h-4" /> 감정 비율
            </h4>
            <div className="space-y-3">
              {emotionCounts.slice(0, 4).map(emo => (
                <div key={emo.id}>
                  <div className="flex justify-between text-xs mb-1 text-gray-300">
                    <span>{emo.label}</span>
                    <span>{Math.round((emo.count / total) * 100)}%</span>
                  </div>
                  <div className="w-full h-3 bg-gray-800 rounded-full overflow-hidden">
                    <div className={`h-full ${emo.color}`} style={{ width: `${(emo.count / total) * 100}%` }}></div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="relative z-10 mt-6 bg-gray-800/50 p-4 rounded-xl border border-gray-700">
            <div className="flex items-center gap-2 mb-2 text-indigo-400 text-xs font-bold uppercase">
              <TrendingUp className="w-3 h-3" /> AI Insight
            </div>
            <p className="text-gray-300 text-xs leading-relaxed">
              "이번 달은 긍정적인 감정이 {Math.round(((emotionCounts.find(e => e.id === 'joy')?.count || 0) + (emotionCounts.find(e => e.id === 'calm')?.count || 0)) / total * 100)}%를 차지했어요. 꾸준히 기록해 볼까요?"
            </p>
          </div>

        </div>

        <div className="mt-6">
          <Button variant="primary" icon={isSaving ? Loader2 : Download} onClick={handleSaveAndShare} disabled={isSaving} className="bg-white text-gray-900 hover:bg-gray-100 shadow-none border-none">
            {isSaving ? '저장 중...' : '저장 및 공유'}
          </Button>
        </div>
      </div>
    </div>
  );
};