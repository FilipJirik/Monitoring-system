import { useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';
import {
    LayoutDashboard,
    Server,
    Bell,
    Users,
    Settings,
    LogOut,
    PanelLeftClose,
    PanelLeft,
    Monitor,
    ChevronDown,
    UserCircle,
    ExternalLink,
    Cog,
    FileCode2,
} from 'lucide-react';
import GlobalAlertListener from '../common/GlobalAlertListener';

const baseNavItems = [
    { to: '/', label: 'Dashboard', icon: LayoutDashboard, adminOnly: false },
    { to: '/devices', label: 'Devices', icon: Server, adminOnly: false },
    { to: '/alerts', label: 'Alerts', icon: Bell, adminOnly: false },
    { to: '/users', label: 'Users', icon: Users, adminOnly: true },
    { to: '/settings', label: 'Settings', icon: Settings, adminOnly: true },
];

export default function MainLayout() {
    const { user, logout } = useAuthStore();
    const navigate = useNavigate();
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
    const [userMenuOpen, setUserMenuOpen] = useState(false);

    const isAdmin = user?.role === 'ADMIN';
    const navItems = baseNavItems.filter((item) => !item.adminOnly || isAdmin);

    const handleLogout = async () => {
        await logout();
        navigate('/login', { replace: true });
    };

    return (
        <div className="min-h-screen bg-zinc-950 flex">
            <GlobalAlertListener />
            {/* ── Sidebar ──────────────────────────────────────────────────────── */}
            <aside
                className={`fixed inset-y-0 left-0 z-30 flex flex-col border-r border-zinc-800/80 bg-zinc-950 transition-all duration-300 ${sidebarCollapsed ? 'w-16' : 'w-60'
                    }`}
            >
                {/* Logo */}
                <div className="flex items-center gap-3 h-14 px-4 border-b border-zinc-800/80 shrink-0">
                    <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-zinc-800/80 border border-zinc-700/50 shrink-0">
                        <Monitor className="w-4 h-4 text-zinc-300" />
                    </div>
                    {!sidebarCollapsed && (
                        <span className="text-sm font-semibold text-zinc-200 tracking-tight truncate">
                            Device Monitor
                        </span>
                    )}
                </div>

                {/* Navigation */}
                <nav className="flex-1 py-3 px-2 space-y-0.5 overflow-y-auto">
                    {navItems.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            end={item.to === '/'}
                            className={({ isActive }) =>
                                `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors duration-150 group
                ${isActive
                                    ? 'bg-zinc-800/70 text-zinc-100'
                                    : 'text-zinc-500 hover:text-zinc-300 hover:bg-zinc-900'
                                }
                ${sidebarCollapsed ? 'justify-center' : ''}`
                            }
                            title={sidebarCollapsed ? item.label : undefined}
                        >
                            <item.icon
                                className={`w-4.5 h-4.5 shrink-0 ${sidebarCollapsed ? '' : ''}`}
                            />
                            {!sidebarCollapsed && <span className="truncate">{item.label}</span>}
                        </NavLink>
                    ))}

                    {/* Admin-only external tools */}
                    {isAdmin && (
                        <>
                            <div className={`my-2 border-t border-zinc-800/60 ${sidebarCollapsed ? 'mx-1' : 'mx-3'}`} />
                            {!sidebarCollapsed && (
                                <p className="px-3 mb-1 text-[10px] font-medium uppercase tracking-widest text-zinc-600">Dev Tools</p>
                            )}
                            <a
                                href={`http://${window.location.hostname}:8000/dashboard/`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors duration-150 text-zinc-500 hover:text-zinc-300 hover:bg-zinc-900 ${sidebarCollapsed ? 'justify-center' : ''}`}
                                title={sidebarCollapsed ? 'JobRunr' : undefined}
                            >
                                <Cog className="w-4.5 h-4.5 shrink-0" />
                                {!sidebarCollapsed && (
                                    <>
                                        <span className="truncate">JobRunr</span>
                                        <ExternalLink className="w-3 h-3 ml-auto text-zinc-700" />
                                    </>
                                )}
                            </a>
                            <a
                                href={`http://${window.location.hostname}:8080/swagger-ui/index.html`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className={`flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors duration-150 text-zinc-500 hover:text-zinc-300 hover:bg-zinc-900 ${sidebarCollapsed ? 'justify-center' : ''}`}
                                title={sidebarCollapsed ? 'Swagger' : undefined}
                            >
                                <FileCode2 className="w-4.5 h-4.5 shrink-0" />
                                {!sidebarCollapsed && (
                                    <>
                                        <span className="truncate">Swagger</span>
                                        <ExternalLink className="w-3 h-3 ml-auto text-zinc-700" />
                                    </>
                                )}
                            </a>
                        </>
                    )}
                </nav>

                {/* Collapse toggle */}
                <div className="px-2 py-2 border-t border-zinc-800/80">
                    <button
                        onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
                        className="flex items-center justify-center w-full px-3 py-2 rounded-lg text-zinc-600 hover:text-zinc-400 hover:bg-zinc-900 transition-colors text-sm cursor-pointer"
                        title={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
                    >
                        {sidebarCollapsed ? (
                            <PanelLeft className="w-4 h-4" />
                        ) : (
                            <>
                                <PanelLeftClose className="w-4 h-4" />
                                <span className="ml-3 truncate">Collapse</span>
                            </>
                        )}
                    </button>
                </div>
            </aside>

            {/* ── Main content ─────────────────────────────────────────────────── */}
            <div
                className={`flex-1 flex flex-col transition-all duration-300 ${sidebarCollapsed ? 'ml-16' : 'ml-60'
                    }`}
            >
                {/* Header */}
                <header className="sticky top-0 z-20 h-14 flex items-center justify-between px-6 border-b border-zinc-800/80 bg-zinc-950/80 backdrop-blur-md">
                    <div />

                    {/* User dropdown */}
                    <div className="relative">
                        <button
                            onClick={() => setUserMenuOpen(!userMenuOpen)}
                            className="flex items-center gap-2.5 px-2.5 py-1.5 -mr-2 rounded-lg hover:bg-zinc-900 transition-colors cursor-pointer"
                        >
                            <div className="w-7 h-7 rounded-full bg-zinc-800 border border-zinc-700/60 flex items-center justify-center text-xs font-semibold text-zinc-300 uppercase">
                                {user?.username?.charAt(0) ?? '?'}
                            </div>
                            <span className="text-sm text-zinc-300 font-medium hidden sm:inline">
                                {user?.username}
                            </span>
                            <ChevronDown className="w-3.5 h-3.5 text-zinc-600" />
                        </button>

                        {userMenuOpen && (
                            <>
                                {/* Backdrop */}
                                <div
                                    className="fixed inset-0 z-40"
                                    onClick={() => setUserMenuOpen(false)}
                                />
                                {/* Dropdown */}
                                <div className="absolute right-0 mt-2 w-56 z-50 bg-zinc-900 border border-zinc-800 rounded-xl shadow-xl shadow-black/30 py-1.5 animate-in fade-in slide-in-from-top-1 duration-150">
                                    <div className="px-3 py-2.5 border-b border-zinc-800/80">
                                        <p className="text-sm font-medium text-zinc-200 truncate">
                                            {user?.username}
                                        </p>
                                        <p className="text-xs text-zinc-500 truncate mt-0.5">
                                            {user?.email}
                                        </p>
                                    </div>
                                    <button
                                        onClick={() => { setUserMenuOpen(false); navigate('/profile'); }}
                                        className="flex items-center gap-2.5 w-full px-3 py-2 mt-1 text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/60 transition-colors cursor-pointer"
                                    >
                                        <UserCircle className="w-4 h-4" />
                                        Profile
                                    </button>
                                    <button
                                        onClick={handleLogout}
                                        className="flex items-center gap-2.5 w-full px-3 py-2 text-sm text-zinc-400 hover:text-zinc-200 hover:bg-zinc-800/60 transition-colors cursor-pointer"
                                    >
                                        <LogOut className="w-4 h-4" />
                                        Sign out
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </header>

                {/* Page content */}
                <main className="flex-1 p-6">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}
