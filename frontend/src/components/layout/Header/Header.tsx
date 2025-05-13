import React from 'react';
import './Header.css';

type HeaderProps = object

const Header: React.FC<HeaderProps> = () => {
    return (
        <header className="app-header">
            <div className="header-left">
                <h1 className="app-title">Domain Sleuth</h1>
            </div>
        </header>
    );
};

export default Header;